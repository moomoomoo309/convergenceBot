# Dynamically Loading from a jar at runtime
### See PluginLoader.kt.
#### Current process:
- When the jar is built, the resources folder should contain a file called MainClass.txt to specify which class contains the plugin.
- If that file is found, it will load the plugin, register it into the core, and everything should work as expected without any weird casting issues.

# Interface Specs
See interfaces.kt.