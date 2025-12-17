# Hytale Performance Saver Plugin
This plugin ensures stability of a Hytale server by lowering the resource consumption of the server when it is under
resource pressure.

## Purpose of this Plugin
The resource usage of a Hytale server, even without mods, can fluctuate heavily based on player behavior. For example,
a large group of players in a small area has a relatively small resource footprint, whereas a small amount of players
each independently exploring the world can cause a significant amount of CPU load and RAM consumption.

It is not always reasonable or cost-effective to run a Hytale game server on the hardware specifications it _might_
need when players do unexpected things, since that means that those hardware resources will remain unused for the vast
majority of time. However, simply running a game server on limited resources may lead to bad performance or server
crashes in these rare high load scenarios.

This plugin's primary goal is to handle resource pressure in an intelligent manner that keeps the game enjoyable for
players.

## Main Features
The plugin takes the following measures to optimize resource usage:

### TPS Limiting
Based on how networking and client prediction work, lower, but stable TPS is generally better for the player
experience than high, but fluctuating TPS. The plugin allows to limit the server TPS to a configurable amount
(default: 20 TPS).

The plugin also limits the TPS of a server that is empty (default: 5).

### Dynamic View Radius Adjustment
The plugin detects CPU pressure through low TPS, and RAM pressure by observing the JVM's garbage collection attempts.
If either resource is under pressure, the view radius is dynamically adjusted to free up those resources again. When
the resources recover, the view radius is gradually increased again. This measure is able to prevent resource-related
server crashes even under stress test scenarios.

### Additional Garbage Collection
Java generally does not free up unused memory on its own. This plugin therefore observes the number of loaded chunks
and explicitly triggers an additional garbage collection if it is highly likely that memory can be freed up.

## Usage

### Installation
Place the plugin JAR into your server's `plugins/` folder.

### Configuration
[[ TODO ]]

## Contributing
Community contributions are welcome and encouraged.

### Security
If you believe to have found a security vulnerability, please report your findings via security@nitrado.net.