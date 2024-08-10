import torch
from models.cycle_gan_model import CycleGANModel
from options.train_options import TrainOptions

# Parse training options
opt = TrainOptions().parse()

# Create a new CycleGANModel instance
model = CycleGANModel(opt)

# Load the .pth file containing the model parameters
model_params = torch.load('latest_net_G.pth')

# Load the entire model from the .pth file
model = model_params

# Save the entire model instance as a .pt file
torch.save(model, 'your_model.pt')
