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
| P20 | 1.20.5's chunk toasts (stream presentation1205) | `ChunkProviderServer.saveChunkData` / `func_73242_b` `(Lnet/minecraft/world/chunk/Chunk;)V` and `loadChunkFromFile` / `func_73239_e` `(II)Lnet/minecraft/world/chunk/Chunk;` | First line: `ChunkFailures.saving(chunk)` / `ChunkFailures.loading(x, z)`, which only remember the chunk (Forge-patched class, javac frames; parameters only) | The failures are caught and logged inside these methods; the log lines name no chunk, and no event fires before the catch |
