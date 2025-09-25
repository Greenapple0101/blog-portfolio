const mongoose = require('mongoose');

const projectSchema = new mongoose.Schema({
  title: {
    type: String,
    required: [true, 'Project title is required'],
    trim: true,
    maxlength: [100, 'Title cannot exceed 100 characters']
  },
  description: {
    type: String,
    required: [true, 'Project description is required'],
    maxlength: [1000, 'Description cannot exceed 1000 characters']
  },
  longDescription: {
    type: String,
    maxlength: [5000, 'Long description cannot exceed 5000 characters']
  },
  images: [{
    url: String,
    alt: String,
    isMain: { type: Boolean, default: false }
  }],
  technologies: [{
    type: String,
    trim: true
  }],
  category: {
    type: String,
    required: [true, 'Project category is required'],
    enum: ['web', 'mobile', 'desktop', 'game', 'other']
  },
  status: {
    type: String,
    enum: ['completed', 'in-progress', 'planned'],
    default: 'completed'
  },
  links: {
    live: String,
    github: String,
    demo: String
  },
  featured: {
    type: Boolean,
    default: false
  },
  order: {
    type: Number,
    default: 0
  },
  startDate: Date,
  endDate: Date,
  author: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  }
}, {
  timestamps: true
});

// Index for better search performance
projectSchema.index({ title: 'text', description: 'text', technologies: 'text' });
projectSchema.index({ category: 1, featured: 1, order: 1 });

module.exports = mongoose.model('Project', projectSchema);
