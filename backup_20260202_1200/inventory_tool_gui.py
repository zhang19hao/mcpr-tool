import os
import sys
import subprocess
import threading
import tkinter as tk
from tkinter import filedialog, messagebox

from generate_inventory_commands import generate_datapack

def resource_path(name):
    base = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(base, name)

def find_jar():
    candidates = [
        resource_path("inventory-tool.jar"),
        os.path.join(os.path.dirname(os.path.abspath(__file__)), "ReplayMod", "libs", "ReplayStudio", "build", "libs", "inventory-tool.jar"),
    ]
    for path in candidates:
        if os.path.exists(path):
            return path
    return None

def open_dir(path):
    if os.path.exists(path):
        os.startfile(path)

def parse_time_to_ms(hours, minutes, seconds, use_time):
    if not use_time:
        return -1
    total = hours * 3600 + minutes * 60 + seconds
    return total * 1000

class InventoryToolApp:
    def __init__(self, root):
        self.root = root
        self.root.title("MCPR 背包工具")
        self.mcpr_path = tk.StringVar()
        self.output_dir = tk.StringVar()
        self.version = tk.StringVar(value="1.20.1")
        self.pack_name = tk.StringVar(value="InventoryRestore")
        self.namespace = tk.StringVar(value="inventory_restore")
        self.function_name = tk.StringVar(value="restore")
        self.use_time = tk.BooleanVar(value=True)
        self.hours = tk.IntVar(value=0)
        self.minutes = tk.IntVar(value=45)
        self.seconds = tk.IntVar(value=58)
        self.build_ui()

    def build_ui(self):
        frame = tk.Frame(self.root, padx=12, pady=12)
        frame.grid(row=0, column=0, sticky="nsew")

        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)
        frame.columnconfigure(1, weight=1)

        tk.Label(frame, text="回放文件").grid(row=0, column=0, sticky="w")
        tk.Entry(frame, textvariable=self.mcpr_path).grid(row=0, column=1, sticky="ew", padx=6)
        tk.Button(frame, text="选择", command=self.choose_mcpr).grid(row=0, column=2, sticky="e")

        tk.Label(frame, text="输出目录").grid(row=1, column=0, sticky="w", pady=(6, 0))
        tk.Entry(frame, textvariable=self.output_dir).grid(row=1, column=1, sticky="ew", padx=6, pady=(6, 0))
        tk.Button(frame, text="选择", command=self.choose_output).grid(row=1, column=2, sticky="e", pady=(6, 0))

        time_frame = tk.Frame(frame)
        time_frame.grid(row=2, column=1, sticky="w", padx=6, pady=(6, 0))
        tk.Checkbutton(frame, text="使用时间", variable=self.use_time).grid(row=2, column=0, sticky="w", pady=(6, 0))
        tk.Spinbox(time_frame, from_=0, to=23, width=4, textvariable=self.hours).grid(row=0, column=0)
        tk.Label(time_frame, text="时").grid(row=0, column=1, padx=(2, 8))
        tk.Spinbox(time_frame, from_=0, to=59, width=4, textvariable=self.minutes).grid(row=0, column=2)
        tk.Label(time_frame, text="分").grid(row=0, column=3, padx=(2, 8))
        tk.Spinbox(time_frame, from_=0, to=59, width=4, textvariable=self.seconds).grid(row=0, column=4)
        tk.Label(time_frame, text="秒").grid(row=0, column=5, padx=(2, 0))

        tk.Label(frame, text="游戏版本").grid(row=3, column=0, sticky="w", pady=(6, 0))
        tk.Entry(frame, textvariable=self.version).grid(row=3, column=1, sticky="ew", padx=6, pady=(6, 0))

        tk.Label(frame, text="数据包名称").grid(row=4, column=0, sticky="w", pady=(6, 0))
        tk.Entry(frame, textvariable=self.pack_name).grid(row=4, column=1, sticky="ew", padx=6, pady=(6, 0))

        tk.Label(frame, text="命名空间").grid(row=5, column=0, sticky="w", pady=(6, 0))
        tk.Entry(frame, textvariable=self.namespace).grid(row=5, column=1, sticky="ew", padx=6, pady=(6, 0))

        tk.Label(frame, text="函数名").grid(row=6, column=0, sticky="w", pady=(6, 0))
        tk.Entry(frame, textvariable=self.function_name).grid(row=6, column=1, sticky="ew", padx=6, pady=(6, 0))

        action_frame = tk.Frame(frame)
        action_frame.grid(row=7, column=1, sticky="w", padx=6, pady=(10, 0))
        tk.Button(action_frame, text="开始生成", command=self.run).grid(row=0, column=0)
        tk.Button(action_frame, text="打开输出目录", command=self.open_output).grid(row=0, column=1, padx=(8, 0))

        self.log = tk.Text(frame, height=12, wrap="word", state="disabled")
        self.log.grid(row=8, column=0, columnspan=3, sticky="nsew", pady=(10, 0))
        frame.rowconfigure(8, weight=1)

    def choose_mcpr(self):
        path = filedialog.askopenfilename(filetypes=[("ReplayMod 文件", "*.mcpr")])
        if path:
            self.mcpr_path.set(path)
            if not self.output_dir.get():
                self.output_dir.set(os.path.dirname(path))

    def choose_output(self):
        path = filedialog.askdirectory()
        if path:
            self.output_dir.set(path)

    def append_log(self, text):
        self.log.configure(state="normal")
        self.log.insert("end", text + "\n")
        self.log.see("end")
        self.log.configure(state="disabled")

    def open_output(self):
        path = self.output_dir.get().strip()
        if path:
            open_dir(path)

    def run(self):
        thread = threading.Thread(target=self.run_worker, daemon=True)
        thread.start()

    def run_worker(self):
        mcpr = self.mcpr_path.get().strip()
        if not mcpr or not os.path.exists(mcpr):
            messagebox.showerror("错误", "请选择有效的 MCPR 文件")
            return
        output_dir = self.output_dir.get().strip() or os.path.dirname(mcpr)
        os.makedirs(output_dir, exist_ok=True)

        timestamp = parse_time_to_ms(self.hours.get(), self.minutes.get(), self.seconds.get(), self.use_time.get())
        version = self.version.get().strip()
        jar = find_jar()
        if jar is None:
            messagebox.showerror("错误", "未找到 inventory-tool.jar，请先执行构建")
            return

        args = ["java", "-jar", jar, mcpr]
        if timestamp >= 0:
            args.append(str(timestamp))
        if version:
            args.append(version)

        self.append_log("开始提取背包数据")
        try:
            result = subprocess.run(args, capture_output=True, text=True, cwd=os.path.dirname(jar))
            if result.stdout:
                self.append_log(result.stdout.strip())
            if result.stderr:
                self.append_log(result.stderr.strip())
            if result.returncode != 0:
                messagebox.showerror("错误", "提取失败，请查看日志")
                return
        except Exception as e:
            messagebox.showerror("错误", str(e))
            return

        if timestamp >= 0:
            json_path = mcpr + "." + str(timestamp) + ".inventory.json"
        else:
            json_path = mcpr + ".inventory.json"

        if not os.path.exists(json_path):
            messagebox.showerror("错误", "未找到生成的 inventory.json")
            return

        pack_name = self.pack_name.get().strip() or "InventoryRestore"
        namespace = self.namespace.get().strip() or "inventory_restore"
        function_name = self.function_name.get().strip() or "restore"

        result = generate_datapack(json_path, output_dir, pack_name, namespace, function_name)
        if result is None:
            messagebox.showerror("错误", "生成 function 包失败")
            return

        self.append_log("已生成数据包: " + result["pack_dir"])
        self.append_log("函数文件: " + result["function_path"])
        messagebox.showinfo("完成", "生成完成")

def main():
    root = tk.Tk()
    app = InventoryToolApp(root)
    root.mainloop()

if __name__ == "__main__":
    main()
