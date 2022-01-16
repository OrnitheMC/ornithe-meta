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

### /v2/versions/game/intermediary

Lists all of the compatible game versions for intermediary.

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

### /v2/versions/intermediary

Lists all of the intermediary versions, stable is based of the Minecraft version.

```json
[
  {
    "maven": "me.copetan:intermediary:13w41a",
    "version": "13w41a",
    "stable": false
  },
  {
    "maven": "me.copetan:intermediary:13w39b",
    "version": "13w39b",
    "stable": false
  }
]
```

### /v2/versions/intermediary/:game_version

Lists all of the intermediary for the provided game version, there will only ever be 1.

```json
[
  {
    "maven": "me.copetan:intermediary:1.7.2",
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
    "separator": "+build.",
    "build": 132,
    "maven": "net.fabricmc:fabric-loader:0.4.2+build.132",
    "version": "0.4.2+build.132",
    "stable": true
  },
  {
    "separator": "+build.",
    "build": 131,
    "maven": "net.fabricmc:fabric-loader:0.4.2+build.131",
    "version": "0.4.2+build.131",
    "stable": false
  }
]
```

### /v2/versions/loader/:game_version

This returns a list of all the compatible loader versions for a given version of the game, along with the best version of intermediary to use for that version.

```json
[
  {
    "loader": {
      "separator": "+build.",
      "build": 155,
      "maven": "net.fabricmc:fabric-loader:0.4.8+build.155",
      "version": "0.4.8+build.155",
      "stable": true
    },
    "intermediary": {
      "maven": "me.copetan:intermediary:1.7.2",
      "version": "1.7.2",
      "stable": true
    }
  },
  {
    "loader": {
      "separator": "+build.",
      "build": 154,
      "maven": "net.fabricmc:fabric-loader:0.4.8+build.154",
      "version": "0.4.8+build.154",
      "stable": false
    },
    "intermediary": {
      "maven": "me.copetan:intermediary:1.7.2",
      "version": "1.7.2",
      "stable": true
    }
  }
]
```

### /v2/versions/loader/:game_version/:loader_version

This returns the best intermediary for the supplied Minecraft version, as well as the details for the supplied loader version. This should be used if you want to install a specific version of loader along with some intermediary for a specific game version.

Since version 0.1.1 `launcherMeta` is now included, this can be used to get the libraries required by fabric-loader as well as the main class for each side.

```json
{
  "loader": {
    "separator": "+build.",
    "build": 155,
    "maven": "net.fabricmc:fabric-loader:0.4.8+build.155",
    "version": "0.4.8+build.155",
    "stable": true
  },
  "intermediary": {
    "maven": "net.fabricmc:intermediary:1.7.2",
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
          "name": "net.fabricmc:tiny-mappings-parser:0.1.1.8",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "net.fabricmc:sponge-mixin:0.7.11.36",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "net.fabricmc:tiny-remapper:0.1.0.33",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "net.fabricmc:fabric-loader-sat4j:2.3.5.4",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "com.google.jimfs:jimfs:1.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm:7.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-analysis:7.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-commons:7.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-tree:7.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-util:7.1",
          "url": "https://maven.fabricmc.net/"
        }
      ],
      "server": [
        {
          "_comment": "jimfs in fabric-server-launch requires guava on the system classloader",
          "name": "com.google.guava:guava:21.0",
          "url": "https://maven.fabricmc.net/"
        }
      ]
    },
    "mainClass": {
      "client": "net.fabricmc.loader.launch.knot.KnotClient",
      "server": "net.fabricmc.loader.launch.knot.KnotServer"
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
