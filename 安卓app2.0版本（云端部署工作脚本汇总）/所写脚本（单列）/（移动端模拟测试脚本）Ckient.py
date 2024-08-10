import base64
import requests
from io import BytesIO
from PIL import Image

def send_image_to_server(image_path, url):
    # 读取图像文件并转换为Base64编码
    with open(image_path, "rb") as image_file:
        encoded_string = base64.b64encode(image_file.read()).decode()

    # 构建请求数据
    data = {'image': encoded_string}

    # 发送POST请求到服务器
    response = requests.post(url, data=data)

    # 处理响应
    if response.status_code == 200:
        print("Request successful.")
        response_data = response.json()
        # print(response_data['info'])  # 打印服务器返回的其他信息

        # 解码图像
        img_data = base64.b64decode(response_data['image'])
        img = Image.open(BytesIO(img_data))
        img.save('returned_image.jpg')  # 保存图像到本地
        print("Image saved as 'returned_image.jpg'.")
    else:
        print(f"Request failed. Status code: {response.status_code}")
        print(response.text)

if __name__ == "__main__":
    image_path = "src.jpg"  # 替换为您要发送的图像文件的路径
    url = "http://localhost:10010/"  # Flask服务器地址
    # url = "http://49.232.151.58:10010/"  # Flask服务器地址
    send_image_to_server(image_path, url)
