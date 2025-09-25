const express = require('express');
const nodemailer = require('nodemailer');
const Contact = require('../models/Contact');
const { authenticateToken, requireAdmin } = require('../middleware/auth');

const router = express.Router();

// Configure email transporter
const createTransporter = () => {
  return nodemailer.createTransporter({
    host: process.env.EMAIL_HOST,
    port: process.env.EMAIL_PORT,
    secure: false, // true for 465, false for other ports
    auth: {
      user: process.env.EMAIL_USER,
      pass: process.env.EMAIL_PASS,
    },
  });
};

// Submit contact form (public)
router.post('/', async (req, res) => {
  try {
    const { name, email, subject, message, phone, company } = req.body;

    // Create contact record
    const contact = new Contact({
      name,
      email,
      subject,
      message,
      phone,
      company,
      source: 'website'
    });

    await contact.save();

    // Send email notification
    try {
      const transporter = createTransporter();
      
      const mailOptions = {
        from: process.env.EMAIL_USER,
        to: process.env.EMAIL_USER, // Send to yourself
        subject: `New Contact Form Submission: ${subject}`,
        html: `
          <h2>New Contact Form Submission</h2>
          <p><strong>Name:</strong> ${name}</p>
          <p><strong>Email:</strong> ${email}</p>
          ${phone ? `<p><strong>Phone:</strong> ${phone}</p>` : ''}
          ${company ? `<p><strong>Company:</strong> ${company}</p>` : ''}
          <p><strong>Subject:</strong> ${subject}</p>
          <p><strong>Message:</strong></p>
          <p>${message.replace(/\n/g, '<br>')}</p>
          <hr>
          <p><small>Submitted at: ${new Date().toLocaleString()}</small></p>
        `
      };

      await transporter.sendMail(mailOptions);

      // Send auto-reply to user
      const autoReplyOptions = {
        from: process.env.EMAIL_USER,
        to: email,
        subject: 'Thank you for contacting me!',
        html: `
          <h2>Thank you for your message!</h2>
          <p>Hi ${name},</p>
          <p>Thank you for reaching out to me. I have received your message and will get back to you as soon as possible.</p>
          <p><strong>Your message:</strong></p>
          <p>${message.replace(/\n/g, '<br>')}</p>
          <hr>
          <p>Best regards,<br>Your Name</p>
        `
      };

      await transporter.sendMail(autoReplyOptions);

    } catch (emailError) {
      console.error('Email sending error:', emailError);
      // Don't fail the request if email fails
    }

    res.status(201).json({
      message: 'Message sent successfully! I will get back to you soon.',
      contactId: contact._id
    });
  } catch (error) {
    console.error('Contact form error:', error);
    res.status(500).json({ message: 'Failed to send message' });
  }
});

// Get all contact messages (admin only)
router.get('/', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const {
      page = 1,
      limit = 20,
      status,
      priority,
      search
    } = req.query;

    const query = {};

    // Filter by status
    if (status) {
      query.status = status;
    }

    // Filter by priority
    if (priority) {
      query.priority = priority;
    }

    // Search functionality
    if (search) {
      query.$or = [
        { name: { $regex: search, $options: 'i' } },
        { email: { $regex: search, $options: 'i' } },
        { subject: { $regex: search, $options: 'i' } },
        { message: { $regex: search, $options: 'i' } }
      ];
    }

    const contacts = await Contact.find(query)
      .sort({ createdAt: -1 })
      .limit(limit * 1)
      .skip((page - 1) * limit)
      .exec();

    const total = await Contact.countDocuments(query);

    res.json({
      contacts,
      totalPages: Math.ceil(total / limit),
      currentPage: page,
      total
    });
  } catch (error) {
    console.error('Get contacts error:', error);
    res.status(500).json({ message: 'Failed to fetch contacts' });
  }
});

// Get single contact message (admin only)
router.get('/:id', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const contact = await Contact.findById(req.params.id);

    if (!contact) {
      return res.status(404).json({ message: 'Contact not found' });
    }

    // Mark as read if it's new
    if (contact.status === 'new') {
      contact.status = 'read';
      await contact.save();
    }

    res.json(contact);
  } catch (error) {
    console.error('Get contact error:', error);
    res.status(500).json({ message: 'Failed to fetch contact' });
  }
});

// Update contact status (admin only)
router.put('/:id/status', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const { status, priority, tags, notes } = req.body;

    const updateData = {};
    if (status) updateData.status = status;
    if (priority) updateData.priority = priority;
    if (tags) updateData.tags = tags;

    // Add note if provided
    if (notes) {
      updateData.$push = {
        notes: {
          content: notes,
          author: req.user._id,
          createdAt: new Date()
        }
      };
    }

    const contact = await Contact.findByIdAndUpdate(
      req.params.id,
      updateData,
      { new: true, runValidators: true }
    );

    if (!contact) {
      return res.status(404).json({ message: 'Contact not found' });
    }

    res.json({
      message: 'Contact updated successfully',
      contact
    });
  } catch (error) {
    console.error('Update contact error:', error);
    res.status(500).json({ message: 'Failed to update contact' });
  }
});

// Reply to contact (admin only)
router.post('/:id/reply', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const { message, subject } = req.body;

    const contact = await Contact.findById(req.params.id);
    if (!contact) {
      return res.status(404).json({ message: 'Contact not found' });
    }

    // Send reply email
    const transporter = createTransporter();
    
    const mailOptions = {
      from: process.env.EMAIL_USER,
      to: contact.email,
      subject: subject || `Re: ${contact.subject}`,
      html: `
        <h2>Reply to your message</h2>
        <p>Hi ${contact.name},</p>
        <p>${message.replace(/\n/g, '<br>')}</p>
        <hr>
        <p><strong>Original message:</strong></p>
        <p>${contact.message.replace(/\n/g, '<br>')}</p>
        <hr>
        <p>Best regards,<br>Your Name</p>
      `
    };

    await transporter.sendMail(mailOptions);

    // Update contact status
    contact.status = 'replied';
    contact.repliedAt = new Date();
    contact.repliedBy = req.user._id;
    await contact.save();

    res.json({ message: 'Reply sent successfully' });
  } catch (error) {
    console.error('Reply error:', error);
    res.status(500).json({ message: 'Failed to send reply' });
  }
});

// Delete contact (admin only)
router.delete('/:id', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const contact = await Contact.findById(req.params.id);
    
    if (!contact) {
      return res.status(404).json({ message: 'Contact not found' });
    }

    await Contact.findByIdAndDelete(req.params.id);

    res.json({ message: 'Contact deleted successfully' });
  } catch (error) {
    console.error('Delete contact error:', error);
    res.status(500).json({ message: 'Failed to delete contact' });
  }
});

// Get contact statistics (admin only)
router.get('/stats/overview', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const stats = await Contact.aggregate([
      {
        $group: {
          _id: null,
          total: { $sum: 1 },
          new: { $sum: { $cond: [{ $eq: ['$status', 'new'] }, 1, 0] } },
          read: { $sum: { $cond: [{ $eq: ['$status', 'read'] }, 1, 0] } },
          replied: { $sum: { $cond: [{ $eq: ['$status', 'replied'] }, 1, 0] } },
          archived: { $sum: { $cond: [{ $eq: ['$status', 'archived'] }, 1, 0] } }
        }
      }
    ]);

    const monthlyStats = await Contact.aggregate([
      {
        $group: {
          _id: {
            year: { $year: '$createdAt' },
            month: { $month: '$createdAt' }
          },
          count: { $sum: 1 }
        }
      },
      { $sort: { '_id.year': -1, '_id.month': -1 } },
      { $limit: 12 }
    ]);

    res.json({
      overview: stats[0] || { total: 0, new: 0, read: 0, replied: 0, archived: 0 },
      monthly: monthlyStats
    });
  } catch (error) {
    console.error('Get stats error:', error);
    res.status(500).json({ message: 'Failed to fetch statistics' });
  }
});

module.exports = router;
