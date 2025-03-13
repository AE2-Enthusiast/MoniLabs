package net.neganote.monilabs.common.machine.multiblock;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public class PrismaticCrucibleMachine extends WorkableElectricMultiblockMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(PrismaticCrucibleMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private int colorKey;
    public PrismaticCrucibleMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
        colorKey = Color.RED.key;
    }

    @Override
    @NotNull
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (recipe == null) return false;
        if (!recipe.data.contains("required_color") && !recipe.data.contains("colors_tag")) {
            return false;
        }
        if (recipe.data.contains("required_color") && recipe.data.getInt("required_color") != colorKey) {
            return false;
        }
        if (recipe.data.contains("colors_tag")) {
            CompoundTag colorsTag = recipe.data.getCompound("colors_tag");
            int[] inputColorArray = colorsTag.getIntArray("required_colors");
            if (Arrays.stream(inputColorArray).noneMatch(i -> i == colorKey)) {
                return false;
            }
        }

        return super.beforeWorking(recipe);
    }

    @Override
    public void afterWorking() {
        super.afterWorking();
        GTRecipe recipe = recipeLogic.getLastRecipe();
        if (recipe == null) {
            return;
        }

        int newKey = 0;

        ColorChangeMode mode = ColorChangeMode.getModeFromKey(recipe.data.getInt("mode_switch_type"));
        switch (mode) {
            case DETERMINISTIC -> newKey = recipe.data.getInt("result_color");
            case RANDOM_WITH_LIST -> {
                CompoundTag colorsTag = recipe.data.getCompound("colors_tag");
                int[] newPossibleColors = colorsTag.getIntArray("possible_new_colors");
                newKey = Color.getRandomColorFromKeys(newPossibleColors);
            }
            case FULL_RANDOM -> newKey = Color.getRandomColor();
        }
        if (recipe.data.contains("color_change_relative") && recipe.data.getBoolean("color_change_relative")) {
            newKey = (colorKey + newKey) % 12;
        }
        changeColorState(Color.getColorFromKey(newKey));
    }

    private void changeColorState(Color newColor) {
        colorKey = newColor.key;
    }

    public Color getColorState() {
        return Color.getColorFromKey(colorKey);
    }

    public enum ColorChangeMode {
        DETERMINISTIC(0, "monilabs.prismatic.mode_name.deterministic"),
        RANDOM_WITH_LIST(1, "monilabs.prismatic.mode_name.random"),
        FULL_RANDOM(2, "monilabs.prismatic.mode_name.random");

        public static final ColorChangeMode[] MODES = ColorChangeMode.values();

        public final int key;
        public final String nameKey;

        ColorChangeMode(int key, String nameKey) {
            this.key = key;
            this.nameKey = nameKey;
        }

        public static ColorChangeMode getModeFromKey(int pKey) {
            return MODES[pKey];
        }
    }

    public enum Color {
        RED(0, "monilabs.prismatic.color_name.red"),
        ORANGE(1, "monilabs.prismatic.color_name.orange"),
        YELLOW(2, "monilabs.prismatic.color_name.yellow"),
        LIME(3, "monilabs.prismatic.color_name.lime"),
        GREEN(4, "monilabs.prismatic.color_name.green"),
        TEAL(5, "monilabs.prismatic.color_name.turquoise"),
        CYAN(6, "monilabs.prismatic.color_name.cyan"),
        AZURE(7, "monilabs.prismatic.color_name.azure"),
        BLUE(8, "monilabs.prismatic.color_name.blue"),
        INDIGO(9, "monilabs.prismatic.color_name.indigo"),
        MAGENTA(10, "monilabs.prismatic.color_name.magenta"),
        PINK(11, "monilabs.prismatic.color_name.pink");

        public static final Color[] COLORS = Color.values();

        public final String nameKey;
        public final int key;

        Color(int key, String nameKey) {
            this.key = key;
            this.nameKey = nameKey;
        }

        public static Color getColorFromKey(int pKey) {
            return COLORS[pKey];
        }
        public static int getRandomColor() {
            return (int) Math.floor(Math.random() * 12.0);
        }

        public static int getRandomColorFromKeys(int[] keys) {
            return keys[(int) Math.floor(Math.random() * (double) keys.length)];
        }
    }
}
