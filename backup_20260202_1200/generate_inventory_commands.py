import json
import re
import sys
import os

def parse_opennbt_string(s):
    """
    Parses the OpenNBT toString() format into a valid Minecraft SNBT string.
    Example input: "CompoundTag { {Damage=IntTag { 0 }, id=StringTag { minecraft:stick }} }"
    Example output: "{Damage:0,id:\"minecraft:stick\"}"
    """
    s = s.strip()
    
    # Handle CompoundTag
    if s.startswith("CompoundTag { {") and s.endswith("} }"):
        content = s[15:-3] # Remove "CompoundTag { {" and "} }"
        # Split by comma, but respect nested braces
        pairs = split_respecting_brackets(content)
        snbt_pairs = []
        for pair in pairs:
            if "=" in pair:
                key, value = pair.split("=", 1)
                snbt_pairs.append(f"{key.strip()}:{parse_opennbt_string(value)}")
        return "{" + ",".join(snbt_pairs) + "}"
    
    # Handle ListTag
    elif s.startswith("ListTag { [") and s.endswith("] }"):
        content = s[11:-3] # Remove "ListTag { [" and "] }"
        items = split_respecting_brackets(content)
        snbt_items = [parse_opennbt_string(item) for item in items if item.strip()]
        return "[" + ",".join(snbt_items) + "]"
    
    # Handle StringTag
    elif s.startswith("StringTag { ") and s.endswith(" }"):
        val = s[12:-2]
        # Escape quotes if necessary
        val = val.replace('\\', '\\\\').replace('"', '\\"')
        return f'"{val}"'
    
    # Handle IntTag
    elif s.startswith("IntTag { ") and s.endswith(" }"):
        return s[9:-2]
        
    # Handle ShortTag
    elif s.startswith("ShortTag { ") and s.endswith(" }"):
        return s[11:-2] + "s"
        
    # Handle ByteTag
    elif s.startswith("ByteTag { ") and s.endswith(" }"):
        val = s[10:-2]
        # OpenNBT might print true/false for bytes sometimes? Assuming numbers for now based on log
        return val + "b"
        
    # Handle LongTag
    elif s.startswith("LongTag { ") and s.endswith(" }"):
        return s[10:-2] + "L"
        
    # Handle FloatTag
    elif s.startswith("FloatTag { ") and s.endswith(" }"):
        return s[11:-2] + "f"
        
    # Handle DoubleTag
    elif s.startswith("DoubleTag { ") and s.endswith(" }"):
        return s[12:-2] + "d"
    
    # Fallback/Error
    return s

def split_respecting_brackets(s):
    parts = []
    current = []
    depth = 0
    for char in s:
        if char == '{' or char == '[':
            depth += 1
        elif char == '}' or char == ']':
            depth -= 1
        
        if char == ',' and depth == 0:
            parts.append("".join(current).strip())
            current = []
        else:
            current.append(char)
    if current:
        parts.append("".join(current).strip())
    return parts

def get_mc_slot(protocol_id):
    pid = int(protocol_id)
    # 1.20.1 Protocol Slot Mapping for Player Inventory (Window ID 0)
    # 0: Crafting Output
    # 1-4: Crafting Input
    # 5: Helmet -> armor.head
    # 6: Chestplate -> armor.chest
    # 7: Leggings -> armor.legs
    # 8: Boots -> armor.feet
    # 9-35: Main Inventory -> inventory.0 to inventory.26
    # 36-44: Hotbar -> hotbar.0 to hotbar.8
    # 45: Offhand -> weapon.offhand
    
    if pid == 5: return "armor.head"
    if pid == 6: return "armor.chest"
    if pid == 7: return "armor.legs"
    if pid == 8: return "armor.feet"
    if 9 <= pid <= 35: return f"inventory.{pid - 9}"
    if 36 <= pid <= 44: return f"hotbar.{pid - 36}"
    if pid == 45: return "weapon.offhand"
    return None

def generate_commands(json_path):
    try:
        with open(json_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"Error: File not found: {json_path}")
        return None

    inventory = data.get("inventory", {})
    commands = []

    for slot_id, item in inventory.items():
        mc_slot = get_mc_slot(slot_id)
        if not mc_slot:
            print(f"Skipping unknown/unsupported slot ID: {slot_id}")
            continue
            
        item_id = item.get("name", "minecraft:air")
        count = item.get("count", 1)
        nbt_raw = item.get("nbt", "")
        
        nbt_str = ""
        if nbt_raw:
            try:
                snbt = parse_opennbt_string(nbt_raw)
                nbt_str = snbt
            except Exception as e:
                print(f"Error parsing NBT for slot {slot_id}: {e}")
                nbt_str = "" # Fallback to no NBT
        
        # Command format: /item replace entity @s <slot> with <item>[<nbt>] <count>
        # Note: <item> argument in /item replace includes NBT directly attached to ID, e.g. minecraft:stick{foo:1}
        
        item_arg = f"{item_id}{nbt_str}"
        cmd = f"item replace entity @s {mc_slot} with {item_arg} {count}"
        commands.append(cmd)

    return commands

def write_mcfunction(commands, output_path):
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'w', encoding='utf-8') as f:
        for cmd in commands:
            f.write(cmd + "\n")
    return output_path

def write_pack_mcmeta(pack_dir, description):
    os.makedirs(pack_dir, exist_ok=True)
    path = os.path.join(pack_dir, "pack.mcmeta")
    with open(path, 'w', encoding='utf-8') as f:
        json.dump({"pack": {"pack_format": 15, "description": description}}, f, ensure_ascii=False, indent=2)
    return path

def generate_datapack(json_path, output_root, pack_name, namespace, function_name):
    commands = generate_commands(json_path)
    if commands is None:
        return None
    pack_dir = os.path.join(output_root, pack_name)
    function_dir = os.path.join(pack_dir, "data", namespace, "functions")
    function_path = os.path.join(function_dir, f"{function_name}.mcfunction")
    write_mcfunction(commands, function_path)
    write_pack_mcmeta(pack_dir, f"{pack_name} generated by MCPR Inventory Tool")
    return {"pack_dir": pack_dir, "function_path": function_path, "command_count": len(commands)}

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python generate_inventory_commands.py <inventory.json> <output_dir> [pack_name] [namespace] [function_name]")
        sys.exit(1)
    json_file = sys.argv[1]
    output_dir = sys.argv[2]
    pack_name = sys.argv[3] if len(sys.argv) >= 4 else "InventoryRestore"
    namespace = sys.argv[4] if len(sys.argv) >= 5 else "inventory_restore"
    function_name = sys.argv[5] if len(sys.argv) >= 6 else "restore"
    result = generate_datapack(json_file, output_dir, pack_name, namespace, function_name)
    if result is None:
        sys.exit(1)
    print(f"Generated {result['command_count']} commands in {result['function_path']}")
