package net.neganote.monilabs.common.machine.multiblock;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.neganote.monilabs.common.machine.part.XPHatchPartMachine;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class SculkVatMachine extends WorkableElectricMultiblockMachine {

    private final ConditionalSubscriptionHandler xpHatchSubscription;

    @Persisted
    @Getter
    private int xpBuffer = 0;

    @Persisted
    @Getter
    private int timer = 0;

    public SculkVatMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
        this.xpHatchSubscription = new ConditionalSubscriptionHandler(this, this::xpHatchTick, this::isFormed);
    }

    private void xpHatchTick() {
        if (timer == 0 && isWorkingEnabled()) {
            var xpHatch = getParts().stream()
                    .filter(XPHatchPartMachine.class::isInstance)
                    .map(XPHatchPartMachine.class::cast)
                    .toArray(XPHatchPartMachine[]::new)[0];

            var xpTank = (NotifiableFluidTank) xpHatch.getRecipeHandlers().get(0)
                    .getCapability(FluidRecipeCapability.CAP);
            int stored = 0;
            if (!xpTank.isEmpty()) {
                stored = ((FluidStack) xpTank.getContents().get(0)).getAmount();
            }
            xpBuffer = Math.min(FluidType.BUCKET_VOLUME << GTValues.ZPM, xpBuffer + stored);
            xpTank.setFluidInTank(0, FluidStack.EMPTY);

            xpBuffer -= xpBuffer >> 4;
        }
        timer = timer + 1 % 8;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        xpHatchSubscription.updateSubscription();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        xpHatchSubscription.updateSubscription();
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (!super.beforeWorking(recipe)) {
            return false;
        }
        if (recipe == null) {
            return false;
        }

        var data = recipe.data;
        if (!data.contains("minimumXp") || !data.contains("maximumXp")) {
            return true;
        }

        int minimumXp = data.getInt("minimumXp");
        int maximumXp = data.getInt("maximumXp");

        return xpBuffer >= minimumXp && xpBuffer <= maximumXp;
    }

    @Override
    public boolean onWorking() {
        var recipe = getRecipeLogic().getLastRecipe();
        if (recipe != null && recipe.data.contains("minimumXp") && recipe.data.contains("maximumXp")) {
            int minimumXp = recipe.data.getInt("minimumXp");
            int maximumXp = recipe.data.getInt("maximumXp");
            if (!(xpBuffer >= minimumXp && xpBuffer <= maximumXp)) {
                if (recipeLogic.getProgress() > 1) {
                    recipeLogic.setProgress(Math.max(1, recipeLogic.getProgress() - 2));
                }
            }
        }
        return true;
    }
}
