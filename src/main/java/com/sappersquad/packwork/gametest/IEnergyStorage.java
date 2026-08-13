package com.sappersquad.packwork.gametest;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/**
 * TEST-ONLY legacy-shaped view over a Team Reborn {@code EnergyStorage}, named after
 * NeoForge's interface so the energy test bodies read word-for-word across every
 * branch. 1 E = 1 FE; every move runs a root transaction over the same face any
 * Fabric tech mod's cable uses.
 */
public interface IEnergyStorage {

    int receiveEnergy(int toReceive, boolean simulate);

    int extractEnergy(int toExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canReceive();

    boolean canExtract();

    static IEnergyStorage of(team.reborn.energy.api.EnergyStorage storage) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int toReceive, boolean simulate) {
                try (Transaction tx = Transaction.openOuter()) {
                    long moved = storage.insert(toReceive, tx);
                    if (!simulate && moved > 0) tx.commit();
                    return (int) moved;
                }
            }

            @Override
            public int extractEnergy(int toExtract, boolean simulate) {
                try (Transaction tx = Transaction.openOuter()) {
                    long moved = storage.extract(toExtract, tx);
                    if (!simulate && moved > 0) tx.commit();
                    return (int) moved;
                }
            }

            @Override
            public int getEnergyStored() {
                return (int) storage.getAmount();
            }

            @Override
            public int getMaxEnergyStored() {
                return (int) storage.getCapacity();
            }

            @Override
            public boolean canReceive() {
                return storage.supportsInsertion();
            }

            @Override
            public boolean canExtract() {
                return storage.supportsExtraction();
            }
        };
    }
}
