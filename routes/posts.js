const express = require('express');
const Post = require('../models/Post');
const Comment = require('../models/Comment');
const { authenticateToken, requireAdmin, optionalAuth } = require('../middleware/auth');
const { upload, handleUploadError } = require('../middleware/upload');

const router = express.Router();

// Get all posts (public - only published)
router.get('/', optionalAuth, async (req, res) => {
  try {
    const {
      page = 1,
      limit = 10,
      category,
      tag,
      search,
      sort = 'newest'
    } = req.query;

    const query = { status: 'published' };

    // Filter by category
    if (category) {
      query.category = category;
    }

    // Filter by tag
    if (tag) {
      query.tags = { $in: [tag.toLowerCase()] };
    }

    // Search functionality
    if (search) {
      query.$or = [
        { title: { $regex: search, $options: 'i' } },
        { content: { $regex: search, $options: 'i' } },
        { tags: { $in: [new RegExp(search, 'i')] } }
      ];
    }

    // Sort options
    let sortOption = {};
    switch (sort) {
      case 'oldest':
        sortOption = { publishedAt: 1 };
        break;
      case 'title':
        sortOption = { title: 1 };
        break;
      case 'views':
        sortOption = { views: -1 };
        break;
      case 'likes':
        sortOption = { likes: -1 };
        break;
      default:
        sortOption = { publishedAt: -1 };
    }

    const posts = await Post.find(query)
      .populate('author', 'username profile')
      .select('-content') // Don't include full content in list
      .sort(sortOption)
      .limit(limit * 1)
      .skip((page - 1) * limit)
      .exec();

    const total = await Post.countDocuments(query);

    res.json({
      posts,
      totalPages: Math.ceil(total / limit),
      currentPage: page,
      total
    });
  } catch (error) {
    console.error('Get posts error:', error);
    res.status(500).json({ message: 'Failed to fetch posts' });
  }
});

// Get single post by slug (public)
router.get('/slug/:slug', optionalAuth, async (req, res) => {
  try {
    const post = await Post.findOne({ slug: req.params.slug, status: 'published' })
      .populate('author', 'username profile')
      .populate({
        path: 'comments',
        match: { status: 'approved' },
        populate: {
          path: 'replies',
          match: { status: 'approved' }
        }
      });

    if (!post) {
      return res.status(404).json({ message: 'Post not found' });
    }

    // Increment view count
    post.views += 1;
    await post.save();

    res.json(post);
  } catch (error) {
    console.error('Get post error:', error);
    res.status(500).json({ message: 'Failed to fetch post' });
  }
});

// Get single post by ID (admin only)
router.get('/admin/:id', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const post = await Post.findById(req.params.id)
      .populate('author', 'username profile')
      .populate('comments');

    if (!post) {
      return res.status(404).json({ message: 'Post not found' });
    }

    res.json(post);
  } catch (error) {
    console.error('Get post error:', error);
    res.status(500).json({ message: 'Failed to fetch post' });
  }
});

// Create new post (admin only)
router.post('/', authenticateToken, requireAdmin, upload.single('featuredImage'), handleUploadError, async (req, res) => {
  try {
    const postData = req.body;
    
    // Handle featured image
    if (req.file) {
      postData.featuredImage = {
        url: `/uploads/posts/${req.file.filename}`,
        alt: req.file.originalname
      };
    }

    // Parse tags array if it's a string
    if (typeof postData.tags === 'string') {
      postData.tags = postData.tags.split(',').map(tag => tag.trim().toLowerCase());
    }

    // Set published date if publishing
    if (postData.status === 'published' && !postData.publishedAt) {
      postData.publishedAt = new Date();
    }

    postData.author = req.user._id;

    const post = new Post(postData);
    await post.save();

    res.status(201).json({
      message: 'Post created successfully',
      post
    });
  } catch (error) {
    console.error('Create post error:', error);
    res.status(500).json({ message: 'Failed to create post' });
  }
});

// Update post (admin only)
router.put('/:id', authenticateToken, requireAdmin, upload.single('featuredImage'), handleUploadError, async (req, res) => {
  try {
    const post = await Post.findById(req.params.id);
    
    if (!post) {
      return res.status(404).json({ message: 'Post not found' });
    }

    const updateData = req.body;

    // Handle new featured image
    if (req.file) {
      updateData.featuredImage = {
        url: `/uploads/posts/${req.file.filename}`,
        alt: req.file.originalname
      };
    }

    // Parse tags array if it's a string
    if (typeof updateData.tags === 'string') {
      updateData.tags = updateData.tags.split(',').map(tag => tag.trim().toLowerCase());
    }

    // Set published date if publishing for the first time
    if (updateData.status === 'published' && post.status !== 'published' && !updateData.publishedAt) {
      updateData.publishedAt = new Date();
    }

    const updatedPost = await Post.findByIdAndUpdate(
      req.params.id,
      updateData,
      { new: true, runValidators: true }
    ).populate('author', 'username profile');

    res.json({
      message: 'Post updated successfully',
      post: updatedPost
    });
  } catch (error) {
    console.error('Update post error:', error);
    res.status(500).json({ message: 'Failed to update post' });
  }
});

// Delete post (admin only)
router.delete('/:id', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const post = await Post.findById(req.params.id);
    
    if (!post) {
      return res.status(404).json({ message: 'Post not found' });
    }

    // Delete associated comments
    await Comment.deleteMany({ post: req.params.id });

    await Post.findByIdAndDelete(req.params.id);

    res.json({ message: 'Post deleted successfully' });
  } catch (error) {
    console.error('Delete post error:', error);
    res.status(500).json({ message: 'Failed to delete post' });
  }
});

// Add comment to post (public)
router.post('/:id/comments', async (req, res) => {
  try {
    const { content, author, parentComment } = req.body;

    const comment = new Comment({
      content,
      author,
      post: req.params.id,
      parentComment: parentComment || null
    });

    await comment.save();

    // Add comment to post
    await Post.findByIdAndUpdate(req.params.id, {
      $push: { comments: comment._id }
    });

    res.status(201).json({
      message: 'Comment added successfully',
      comment
    });
  } catch (error) {
    console.error('Add comment error:', error);
    res.status(500).json({ message: 'Failed to add comment' });
  }
});

// Get post comments (public)
router.get('/:id/comments', async (req, res) => {
  try {
    const comments = await Comment.find({ 
      post: req.params.id, 
      status: 'approved',
      parentComment: null 
    })
    .populate('replies')
    .sort({ createdAt: -1 });

    res.json(comments);
  } catch (error) {
    console.error('Get comments error:', error);
    res.status(500).json({ message: 'Failed to fetch comments' });
  }
});

// Get post categories
router.get('/meta/categories', (req, res) => {
  const categories = [
    { value: 'tutorial', label: 'Tutorial' },
    { value: 'project', label: 'Project' },
    { value: 'thoughts', label: 'Thoughts' },
    { value: 'news', label: 'News' },
    { value: 'other', label: 'Other' }
  ];
  
  res.json(categories);
});

// Get all tags
router.get('/meta/tags', async (req, res) => {
  try {
    const posts = await Post.find({ status: 'published' }, 'tags');
    const tags = [...new Set(posts.flatMap(p => p.tags))].sort();
    
    res.json(tags);
  } catch (error) {
    console.error('Get tags error:', error);
    res.status(500).json({ message: 'Failed to fetch tags' });
  }
});

module.exports = router;
