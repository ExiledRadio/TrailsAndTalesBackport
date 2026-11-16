# Bytecode patches

The mod's own coremod, `com.exiledradio.trailsandtalesbackport.asm.TrailsAndTalesCore` (sorting index 1001, so every
class arrives with SRG member names in a real instance and MCP names in dev; each patch matches both). One transformer
per patch, no MixinBooter, nothing loaded from inside a transformer. A target of another shape than expected (another
coremod got there first and changed it) is left alone and logged, so vanilla's behavior stays. Inserted code changes no
stack-map frame: no new local, and a parameter is read only on a method's first line (Mojang's unpatched classes keep
ProGuard's frames, which drop a parameter once vanilla stops reading it).

Each patch's hook checks its own config and `Presentation.isAtLeast(...)`, so with the feature off the game behaves as
1.12.2 does. `Asm.APPLIED` lists the patches the running game applied (the self-tests read it).

| # | Feature | Target (MCP / SRG) | Change | Why no event does it |
| --- | --- | --- | --- | --- |
| P20 | 1.20.5 effect particles (`misc.effectParticles`, `effectparticles`) | `EntityLivingBase.updatePotionEffects` / `func_70679_bo` (Forge-patched) | after `dataManager.get(POTION_EFFECTS).intValue()`, `ALOAD 0` + `EffectParticles.effectColor(I, EntityLivingBase)I`: the client draws 1.20.6's per-effect particle and gets 0 back for an entity the server sent a list for; otherwise the color is unchanged | The swirl is drawn inside the entity's own tick from the one blended color; no event runs there, and an event-side particle would leave 1.12.2's blended swirl on top |
