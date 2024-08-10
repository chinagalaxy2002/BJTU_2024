import base64
import os
import cv2
import numpy as np
from flask import request, Flask, jsonify
from datetime import datetime

from INFERENCE import CycleGAN

app = Flask(__name__)

save_path = 'webdata'
if not os.path.exists(save_path):
    os.makedirs(save_path)
img_p = 'rev.jpg'
img_path = os.path.join(save_path, img_p)

# 创建 CycleGAN 的实例
ca = CycleGAN()

def log_request_time():
    """记录每次请求的时间到日志文件"""
    with open('request_log.txt', 'a') as log_file:
        # 获取当前时间并格式化
        current_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log_file.write(f"{current_time}\n")

@app.route("/", methods=['POST'])
def get_frame():
    log_request_time()  # 记录请求时间

    try:
        # 尝试检查是否有图像数据
        if 'image' not in request.form:
            return jsonify({"error": "Missing image data"}), 400

        # 解析图片数据
        img_data = base64.b64decode(request.form['image'])
        image_data = np.frombuffer(img_data, np.uint8)
        image_data = cv2.imdecode(image_data, cv2.IMREAD_COLOR)

        # 确保图像数据被正确解码
        if image_data is None:
            return jsonify({"error": "Could not decode image"}), 400

        cv2.imwrite(img_path, image_data)

    except Exception as e:
        return jsonify({"error": f"Error processing image: {str(e)}"}), 500

    try:
        # 调用 CycleGAN 实例的 detect 方法进行图像处理
        detected_image_pil = ca.detect()
    except Exception as e:
        return jsonify({"error": f"Error in CycleGAN detection: {str(e)}"}), 500

    try:
        # 将 PIL 图像转换为 numpy 数组以编码为 JPEG 格式
        detected_image_np = np.array(detected_image_pil)
        _, img_encoded = cv2.imencode('.jpg', detected_image_np)
        img_base64 = base64.b64encode(img_encoded).decode()
    except Exception as e:
        return jsonify({"error": f"Error encoding image: {str(e)}"}), 500

    # 构造返回结果
    return jsonify({"image": img_base64})

if __name__ == "__main__":
    app.run(threaded=True, host='0.0.0.0', port=10010)
