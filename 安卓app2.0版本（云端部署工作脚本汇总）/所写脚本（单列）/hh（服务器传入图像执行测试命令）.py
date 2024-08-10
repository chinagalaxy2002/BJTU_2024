import subprocess

def run_command():
    # 构建命令行指令
    command = "python test.py --dataroot webdata --name moben_pretrained --model test --no_dropout"
    # 使用subprocess运行命令
    try:
        result = subprocess.run(command.split(), check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        print("命令执行成功，输出如下：")
        print(result.stdout)
    except subprocess.CalledProcessError as e:
        print("命令执行失败，错误信息如下：")
        print(e.stderr)

# 运行函数
run_command()
