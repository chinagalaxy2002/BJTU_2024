import torch
import torch.jit

# 加载训练好的模型
model = torch.load('/home/gxyu/pytorch-CycleGAN-and-pix2pix-master/checkpoints/moben_pretrained/latest_net_G.pth')

# 将模型转换为torchscript
model_script = torch.jit.trace(model, torch.rand(1, 3, 256, 256))  # 这里的输入大小应与模型期望的输入大小匹配

# 保存torchscript模型到文件
model_script.save('moben.torchscript')
