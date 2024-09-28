# Entity Catcher Plugin
## Overview
The **Entity Catcher Plugin** allows players to capture and place entities in custom containers ("Catchers"). Each catcher has different configurations and behaviors such as capturing entity variants, health, equipment, and more. This plugin is fully configurable, allowing server owners to set custom crafting recipes, entity capture restrictions, and special behaviors on entity placement.

---

## Features

- **Custom Items**: Create custom catchers with different materials and lore.
- **Capture Permissions**: Control who can capture/place entities with permission nodes and cooldowns.
- **Entity Data Preservation**: Store entity data like health, armor, custom names, and variants.
- **Custom Place Behaviors**: Define behaviors for placed entities (e.g., AI removal, invincibility).
- **Fully Customizable**: Add and configure catcher types and recipes.

---

## Example Configuration

```yaml
Copia codice
catchers:
  AnimalCatcher:
    display_name: "Animal Catcher"
    description:
      empty:
        material: STICK
        lore:
          - "&7Empty.. go catch something"
      captured:
        material: BLAZE_ROD
        lore:
          - "&7Entity: &f{name}"
          - "&7Type: &f{type}"
          - "&7Age: &f{age}"
          - "&7Variant: &f{variant}"
    recipe:
      shape:
        - " A "
        - " B "
        - " C "
      ingredients:
        A: "IRON_INGOT"
        B: "BUCKET"
        C: "LEATHER"
    capture:
      allowed_types: "ANIMAL"
      permissions:
        capture: "entitycatcher.capture"
        place: "entitycatcher.place"
    capture_data:
      capture_custom_name: true
      capture_health: true
      capture_variant: true
      capture_armor: true
      capture_equipment: true
    place_behavior:
      remove_ai: false
      set_invisible: false
      set_glowing: false
      set_on_fire: true
      set_invincible: true

  MobCatcher:
    display_name: "Mob Catcher"
    description:
      empty:
        material: STICK
        lore:
          - "&7Empty.. go catch something"
      captured:
        material: BLAZE_ROD
        lore:
          - "&7Entity: &f{name}"
          - "&7Type: &f{type}"
          - "&7Age: &f{age}"
          - "&7Variant: &f{variant}"
    recipe:
      shape:
        - " A "
        - " B "
        - " C "
      ingredients:
        A: "IRON_INGOT"
        B: "BUCKET"
        C: "STRING"
    capture:
      allowed_types: "MOB"
      permissions:
        capture: "entitycatcher.capture"
        place: "entitycatcher.place"
    capture_data:
      capture_custom_name: true
      capture_health: true
      capture_equipment: true
    place_behavior:
      remove_ai: false
      set_glowing: false
      set_on_fire: true

  NoAICatcher:
    display_name: "No AI Catcher"
    description:
      empty:
        material: STICK
        lore:
          - "&7Empty.. go catch something"
      captured:
        material: BLAZE_ROD
        lore:
          - "&7Entity: &f{name}"
          - "&7Type: &f{type}"
          - "&7Age: &f{age}"
          - "&7Variant: &f{variant}"
    recipe:
      shape:
        - " A "
        - " B "
        - " C "
      ingredients:
        A: "GOLD_INGOT"
        B: "BUCKET"
        C: "REDSTONE"
    capture:
      allowed_types: "ANYTHING"
      permissions:
        capture: "entitycatcher.capture"
        place: "entitycatcher.place"
    capture_data:
      capture_custom_name: true
      capture_health: true
    place_behavior:
      remove_ai: true
      set_glowing: true
```

## Example Usage

### Crafting a Catcher
1. Use the recipe from the config to craft catchers.
2. Example:
A = IRON_INGOT
B = BUCKET
C = LEATHER

Shape:
" A "
" B "
" C "

### Capturing an Entity
- Right-click with an empty catcher to capture entities. 
- Captured data (name, type, age, variant) appears on the item lore.

### Placing an Entity
- Right-click to place the captured entity. Custom behaviors apply based on the config (e.g., remove AI, invincibility).

---

## Permissions

- `entitycatcher.capture`: Allows capturing entities.
- `entitycatcher.place`: Allows placing captured entities.
- **Cooldowns**: Add numbers to set cooldowns (e.g., `entitycatcher.capture.5` for 5 minutes).

---

## Installation

1. Download the `.jar` and place it in the `plugins` folder.
2. Start the server and modify the `config.yml`.
3. Reload or restart the server.

---
