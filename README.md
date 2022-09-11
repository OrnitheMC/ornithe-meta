# ornithe-meta

Ornithe Meta is a JSON HTTP API that can be used to query metadata about Ornithe's projects. It is updated every 5 mins.

It can be used by tools or launchers that wish to query version information about Ornithe.

Hosted at [https://meta.ornithemc.net/](https://meta.ornithemc.net/)

## Endpoints

The versions are in order, the newest versions appear first.

`game_version` and `loader_version` should be url encoded to allow for special characters. For example `1.7.6 Pre-Release 2` becomes `1.7.6%20Pre-Release%202`

# V2

### /v2/versions

Full database, includes all the data. **Warning**: large JSON.

### /v2/versions/game

Lists all of the supported game versions.

```json
[
  {
    "version": "1.7.2",
    "stable": true
  },
  {
    "version": "1.7.1",
    "stable": false
  }
]
```

### /v2/versions/game/calamus

Lists all of the compatible game versions for calamus.

```json
[
  {
    "version": "13w41a",
    "stable": true
  },
  {
    "version": "13w39b",
    "stable": true
  }
]
```

### /v2/versions/calamus

Lists all of the calamus mappings versions, stable is based of the Minecraft version.

```json
[
  {
    "maven": "net.ornithemc:calamus:13w41a",
    "version": "13w41a",
    "stable": false
  },
  {
    "maven": "net.ornithemc:calamus:13w39b",
    "version": "13w39b",
    "stable": false
  }
]
```

### /v2/versions/calamus/:game_version

Lists all of the calamus mappings for the provided game version, there will only ever be 1.

```json
[
  {
    "maven": "net.ornithemc:calamus:1.7.2",
    "version": "1.7.2",
    "stable": true
  }
]
```

### /v2/versions/loader

Lists all of the loader versions.

```json
[
  {
    "separator": ".",
    "build": 1,
    "maven": "net.ornithemc:ornithe-loader:0.1.1",
    "version": "0.1.1",
    "stable": true
  },
  {
    "separator": ".",
    "build": 2,
    "maven": "net.ornithemc:ornithe-loader:0.1.0",
    "version": "0.1.0",
    "stable": true
  }
]
```

### /v2/versions/loader/:game_version

This returns a list of all the compatible loader versions for a given version of the game, along with the best version of calamus mappings to use for that version.

```json
[
  {
    "loader": {
      "separator": ".",
      "build": 1,
      "maven": "net.ornithemc:ornithe-loader:0.1.1",
      "version": "0.1.1",
      "stable": true
    },
    "calamus": {
      "maven": "net.ornithemc:calamus:1.7.2",
      "version": "1.7.2",
      "stable": true
    }
  },
  {
    "loader": {
      "separator": ".",
      "build": 2,
      "maven": "net.ornithemc:ornithe-loader:0.1.0",
      "version": "0.1.0",
      "stable": true
    },
    "calamus": {
      "maven": "net.ornithemc:calamus:1.7.2",
      "version": "1.7.2",
      "stable": true
    }
  }
]
```

### /v2/versions/loader/:game_version/:loader_version

This returns the best calamus mappings for the supplied Minecraft version, as well as the details for the supplied loader version. This should be used if you want to install a specific version of loader along with some calamus mappings for a specific game version.

Since version 0.1.1 `launcherMeta` is now included, this can be used to get the libraries required by ornithe-loader as well as the main class for each side.

```json
{
  "loader": {
    "separator": ".",
    "build": 1,
    "maven": "net.ornithemc:ornithe-loader:0.1.1",
    "version": "0.1.1",
    "stable": true
  },
  "calamus": {
    "maven": "net.ornithemc:calamus:1.7.2",
    "version": "1.7.2",
    "stable": true
  },
  "launcherMeta": {
    "version": 1,
    "libraries": {
      "client": [
      ],
      "common": [
        {
          "name": "net.minecraft:launchwrapper:1.12"
        },
        {
          "name": "net.ornithemc:nester:0.2.5",
          "url": "https://maven.ornithemc.net/releases"
        },
        {
          "name": "net.fabricmc:tiny-mappings-parser:0.3.0+build.17",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "net.fabricmc:sponge-mixin:0.11.4+mixin.0.8.5",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "net.fabricmc:tiny-remapper:0.8.2",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "net.fabricmc:access-widener:2.1.0",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm:9.3",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-analysis:9.3",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-commons:9.3",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-tree:9.3",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-util:9.3",
          "url": "https://maven.fabricmc.net/"
        }
      ],
      "server": [
      ]
    },
    "mainClass": {
      "client": "net.ornithemc.loader.launch.knot.KnotClient",
      "server": "net.ornithemc.loader.launch.knot.KnotServer"
    }
  }
}
```

### /v2/versions/loader/:game_version/:loader_version/profile/json

Returns the JSON file that should be used in the standard Minecraft launcher.

### /v2/versions/loader/:game_version/:loader_version/profile/zip

Downloads a zip file with the launcher's profile json, and the dummy jar. To be extracted into .minecraft/versions

### /v2/versions/loader/:game_version/:loader_version/server/json

Returns the JSON file in format of the launcher JSON, but with the server's main class.
