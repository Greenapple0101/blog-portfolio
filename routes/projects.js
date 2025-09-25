const express = require('express');
const Project = require('../models/Project');
const { authenticateToken, requireAdmin, optionalAuth } = require('../middleware/auth');
const { upload, handleUploadError } = require('../middleware/upload');

const router = express.Router();

// Get all projects (public)
router.get('/', optionalAuth, async (req, res) => {
  try {
    const {
      page = 1,
      limit = 10,
      category,
      featured,
      search,
      sort = 'order'
    } = req.query;

    const query = {};

    // Filter by category
    if (category) {
      query.category = category;
    }

    // Filter by featured
    if (featured === 'true') {
      query.featured = true;
    }

    // Search functionality
    if (search) {
      query.$or = [
        { title: { $regex: search, $options: 'i' } },
        { description: { $regex: search, $options: 'i' } },
        { technologies: { $in: [new RegExp(search, 'i')] } }
      ];
    }

    // Sort options
    let sortOption = {};
    switch (sort) {
      case 'newest':
        sortOption = { createdAt: -1 };
        break;
      case 'oldest':
        sortOption = { createdAt: 1 };
        break;
      case 'title':
        sortOption = { title: 1 };
        break;
      default:
        sortOption = { order: 1, createdAt: -1 };
    }

    const projects = await Project.find(query)
      .populate('author', 'username profile')
      .sort(sortOption)
      .limit(limit * 1)
      .skip((page - 1) * limit)
      .exec();

    const total = await Project.countDocuments(query);

    res.json({
      projects,
      totalPages: Math.ceil(total / limit),
      currentPage: page,
      total
    });
  } catch (error) {
    console.error('Get projects error:', error);
    res.status(500).json({ message: 'Failed to fetch projects' });
  }
});

// Get single project (public)
router.get('/:id', optionalAuth, async (req, res) => {
  try {
    const project = await Project.findById(req.params.id)
      .populate('author', 'username profile');

    if (!project) {
      return res.status(404).json({ message: 'Project not found' });
    }

    res.json(project);
  } catch (error) {
    console.error('Get project error:', error);
    res.status(500).json({ message: 'Failed to fetch project' });
  }
});

// Create new project (admin only)
router.post('/', authenticateToken, requireAdmin, upload.array('images', 10), handleUploadError, async (req, res) => {
  try {
    const projectData = req.body;
    
    // Handle uploaded images
    if (req.files && req.files.length > 0) {
      projectData.images = req.files.map((file, index) => ({
        url: `/uploads/projects/${file.filename}`,
        alt: file.originalname,
        isMain: index === 0 // First image is main by default
      }));
    }

    // Parse technologies array if it's a string
    if (typeof projectData.technologies === 'string') {
      projectData.technologies = projectData.technologies.split(',').map(tech => tech.trim());
    }

    projectData.author = req.user._id;

    const project = new Project(projectData);
    await project.save();

    res.status(201).json({
      message: 'Project created successfully',
      project
    });
  } catch (error) {
    console.error('Create project error:', error);
    res.status(500).json({ message: 'Failed to create project' });
  }
});

// Update project (admin only)
router.put('/:id', authenticateToken, requireAdmin, upload.array('images', 10), handleUploadError, async (req, res) => {
  try {
    const project = await Project.findById(req.params.id);
    
    if (!project) {
      return res.status(404).json({ message: 'Project not found' });
    }

    const updateData = req.body;

    // Handle new uploaded images
    if (req.files && req.files.length > 0) {
      const newImages = req.files.map((file, index) => ({
        url: `/uploads/projects/${file.filename}`,
        alt: file.originalname,
        isMain: index === 0
      }));
      
      // Merge with existing images or replace
      updateData.images = updateData.replaceImages === 'true' 
        ? newImages 
        : [...(project.images || []), ...newImages];
    }

    // Parse technologies array if it's a string
    if (typeof updateData.technologies === 'string') {
      updateData.technologies = updateData.technologies.split(',').map(tech => tech.trim());
    }

    const updatedProject = await Project.findByIdAndUpdate(
      req.params.id,
      updateData,
      { new: true, runValidators: true }
    ).populate('author', 'username profile');

    res.json({
      message: 'Project updated successfully',
      project: updatedProject
    });
  } catch (error) {
    console.error('Update project error:', error);
    res.status(500).json({ message: 'Failed to update project' });
  }
});

// Delete project (admin only)
router.delete('/:id', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const project = await Project.findById(req.params.id);
    
    if (!project) {
      return res.status(404).json({ message: 'Project not found' });
    }

    await Project.findByIdAndDelete(req.params.id);

    res.json({ message: 'Project deleted successfully' });
  } catch (error) {
    console.error('Delete project error:', error);
    res.status(500).json({ message: 'Failed to delete project' });
  }
});

// Get project categories
router.get('/meta/categories', (req, res) => {
  const categories = [
    { value: 'web', label: 'Web Development' },
    { value: 'mobile', label: 'Mobile Development' },
    { value: 'desktop', label: 'Desktop Application' },
    { value: 'game', label: 'Game Development' },
    { value: 'other', label: 'Other' }
  ];
  
  res.json(categories);
});

// Get project technologies (unique list)
router.get('/meta/technologies', async (req, res) => {
  try {
    const projects = await Project.find({}, 'technologies');
    const technologies = [...new Set(projects.flatMap(p => p.technologies))].sort();
    
    res.json(technologies);
  } catch (error) {
    console.error('Get technologies error:', error);
    res.status(500).json({ message: 'Failed to fetch technologies' });
  }
});

module.exports = router;
