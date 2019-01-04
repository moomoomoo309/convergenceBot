# Dynamically Loading from a jar at runtime
### See PluginLoader.kt.
#### Current process:
- When the jar is built, it should contain a class called Main which extends plugin.
- If that class is found, it will load the plugin, register it into the core, and everything should work as expected without any weird casting issues.

# Interface Specs
See interfaces.kt.