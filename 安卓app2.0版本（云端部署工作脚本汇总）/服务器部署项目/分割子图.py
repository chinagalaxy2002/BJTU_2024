import rasterio
from rasterio import windows
import numpy as np
import os

def split_tif_image(input_tif, output_dir, tile_size=(800, 800)):
    """
    分割TIF图像为指定大小的子图。

    参数:
    input_tif (str): 输入的TIF文件路径。
    output_dir (str): 存储子图的目录路径。
    tile_size (tuple): 子图的大小，格式为(height, width)。
    """
    with rasterio.open(input_tif) as dataset:
        # 计算分割的次数
        width_in_tiles = dataset.width // tile_size[1]
        height_in_tiles = dataset.height // tile_size[0]

        # 创建输出目录
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)

        # 分割图像
        for j in range(height_in_tiles):
            for i in range(width_in_tiles):
                # 计算每个子图的窗口
                window = windows.Window(i*tile_size[1], j*tile_size[0], tile_size[1], tile_size[0])
                transform = windows.transform(window, dataset.transform)

                # 读取窗口内的图像数据
                window_data = dataset.read(window=window)

                # 保存子图
                output_path = os.path.join(output_dir, f"tile_{j}_{i}.png")
                with rasterio.open(
                    output_path, 'w',
                    driver='GTiff',
                    height=tile_size[0], width=tile_size[1],
                    count=dataset.count,
                    dtype=dataset.dtypes[0],
                    crs=dataset.crs,
                    transform=transform,
                ) as dest:
                    dest.write(window_data)

if __name__ == "__main__":
    input_tif = r"E:\BaiduNetdiskDownload/rm.tif"  # 更改为你的输入文件路径
    output_dir = "res"  # 更改为你想要存储子图的目录路径
    split_tif_image(input_tif, output_dir)
