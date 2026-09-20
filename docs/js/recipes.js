/**
 * Fortnite: Save the World - Raw Materials and Trap Recipes definitions.
 * Synchronized with the desktop application and trapRecipes.csv.
 */

const MATERIALS = [
    { id: "mech", key: "MECHANICAL_PARTS", name: "Mechanical Parts", stackSize: 999, image: "images/Efficient_Mechanical_Parts_-_Resource_-_Save_the_World.webp" },
    { id: "twine", key: "TWINE", name: "Twine", stackSize: 999, image: "images/Carved_Twine_-_Resource_-_Save_the_World.webp" },
    { id: "quartz", key: "QUARTZ_CRYSTAL", name: "Quartz Crystal", stackSize: 999, image: "images/Icon_Crystal_White.webp" },
    { id: "tape", key: "DUCT_TAPE", name: "Duct Tape", stackSize: 999, image: "images/Duct_Tape_-_Resource_-_Save_the_World.webp" },
    { id: "powder", key: "MINERAL_POWDER", name: "Mineral Powder", stackSize: 999, image: "images/Oxidized_Mineral_Powder_-_Resource_-_Save_the_World.webp" },
    { id: "bacon", key: "BACON", name: "Bacon", stackSize: 999, image: "images/Bacon_-_Resource_-_Save_the_World.webp" },
    { id: "batteries", key: "BATTERIES", name: "Batteries", stackSize: 999, image: "images/Batteries_-_Resource_-_Save_the_World.webp" },
    { id: "herbs", key: "FIBROUS_HERBS", name: "Fibrous Herbs", stackSize: 999, image: "images/Fibrous_Herbs_-_Resource_-_Save_the_World.webp" },
    { id: "flowers", key: "FLOWER_PETALS", name: "Flower Petals", stackSize: 999, image: "images/Flower_Petals_-_Resource_-_Save_the_World.webp" },
    { id: "nabs", key: "NUTS_AND_BOLTS", name: "Nuts 'n' Bolts", stackSize: 999, image: "images/Icon_Crafting_Tier1_Nut_Bolts.webp" },
    { id: "planks", key: "PLANKS", name: "Planks", stackSize: 999, image: "images/Planks_-_Resource_-_Save_the_World.webp" },
    { id: "rough", key: "ROUGH_ORE", name: "Rough Ore", stackSize: 999, image: "images/Rough_Ore_-_Resource_-_Save_the_World.webp" }
];

const TRAP_RECIPES = [
    {
        id: "anti_air_trap",
        name: "Anti-Air Trap",
        category: "Ceiling / Floor",
        defaultWeight: 5.0,
        ingredients: {
            "Nuts 'n' Bolts": 6,
            "Planks": 3,
            "Quartz Crystal": 1
        }
    },
    {
        id: "broadside",
        name: "Broadside",
        category: "Wall",
        defaultWeight: 5.0,
        ingredients: {
            "Planks": 7,
            "Mechanical Parts": 2,
            "Mineral Powder": 2
        }
    },
    {
        id: "ceiling_drop_trap_wall_darts",
        name: "Ceiling Drop Trap / Wall Darts",
        category: "Ceiling / Wall",
        defaultWeight: 5.0,
        ingredients: {
            "Planks": 7,
            "Mechanical Parts": 2,
            "Twine": 2
        }
    },
    {
        id: "ceiling_electric_field",
        name: "Ceiling Electric Field",
        category: "Ceiling",
        defaultWeight: 5.0,
        ingredients: {
            "Nuts 'n' Bolts": 8,
            "Rough Ore": 3,
            "Batteries": 2,
            "Mineral Powder": 2
        }
    },
    {
        id: "ceiling_gas_trap",
        name: "Ceiling Gas Trap",
        category: "Ceiling",
        defaultWeight: 5.0,
        ingredients: {
            "Nuts 'n' Bolts": 7,
            "Fibrous Herbs": 5,
            "Bacon": 1,
            "Mineral Powder": 2
        }
    },
    {
        id: "ceiling_zapper",
        name: "Ceiling Zapper",
        category: "Ceiling",
        defaultWeight: 5.0,
        ingredients: {
            "Nuts 'n' Bolts": 9,
            "Batteries": 1,
            "Mechanical Parts": 3
        }
    },
    {
        id: "cozy_campfire",
        name: "Cozy Campfire",
        category: "Floor",
        defaultWeight: 5.0,
        ingredients: {
            "Planks": 9,
            "Flower Petals": 2,
            "Twine": 2
        }
    },
    {
        id: "flame_grill_floor_freeze",
        name: "Flame Grill Floor Trap / Floor Freeze Trap",
        category: "Floor",
        defaultWeight: 5.0,
        ingredients: {
            "Rough Ore": 7,
            "Quartz Crystal": 2,
            "Mineral Powder": 2
        }
    },
    {
        id: "floor_launcher_wall_launcher",
        name: "Floor Launcher / Wall Launcher",
        category: "Floor / Wall",
        defaultWeight: 5.0,
        ingredients: {
            "Rough Ore": 4,
            "Planks": 6,
            "Mineral Powder": 2
        }
    },
    {
        id: "healing_pad",
        name: "Healing Pad",
        category: "Floor",
        defaultWeight: 5.0,
        ingredients: {
            "Flower Petals": 4,
            "Fibrous Herbs": 2,
            "Bacon": 1
        }
    },

    {
        id: "retractable_floor_spikes",
        name: "Retractable Floor Spikes",
        category: "Floor",
        defaultWeight: 5.0,
        ingredients: {
            "Nuts 'n' Bolts": 9,
            "Rough Ore": 2,
            "Mechanical Parts": 2
        }
    },
    {
        id: "sound_wall",
        name: "Sound Wall",
        category: "Wall",
        defaultWeight: 5.0,
        ingredients: {
            "Rough Ore": 7,
            "Quartz Crystal": 1,
            "Mechanical Parts": 3
        }
    },
    {
        id: "tar_pit",
        name: "Tar Pit",
        category: "Floor",
        defaultWeight: 5.0,
        ingredients: {
            "Rough Ore": 7,
            "Quartz Crystal": 2,
            "Mechanical Parts": 2
        }
    },
    {
        id: "wall_dynamo",
        name: "Wall Dynamo",
        category: "Wall",
        defaultWeight: 5.0,
        ingredients: {
            "Nuts 'n' Bolts": 8,
            "Batteries": 1,
            "Mechanical Parts": 3
        }
    },
    {
        id: "wall_lights",
        name: "Wall Lights",
        category: "Wall",
        defaultWeight: 5.0,
        ingredients: {
            "Planks": 5,
            "Quartz Crystal": 2,
            "Mechanical Parts": 2
        }
    },
    {
        id: "wall_spikes",
        name: "Wall Spikes",
        category: "Wall",
        defaultWeight: 5.0,
        ingredients: {
            "Planks": 3,
            "Duct Tape": 1,
            "Twine": 2
        }
    },
    {
        id: "wooden_floor_spikes",
        name: "Wooden Floor Spikes",
        category: "Floor",
        defaultWeight: 5.0,
        ingredients: {
            "Planks": 5,
            "Duct Tape": 1,
            "Twine": 2
        }
    }
];

// Helper to calculate slots
function calculateMatSlots(qty) {
    if (!qty || qty <= 0) return 0;
    return Math.ceil(qty / 999);
}

function calculateTrapSlots(qty) {
    if (!qty || qty <= 0) return 0;
    return Math.ceil(qty / 200);
}
