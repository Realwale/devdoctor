# Homebrew packaging

Build the verified local release and formula:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./packaging/build-homebrew-release.sh
brew install --formula ./outputs/devdoctor.rb
brew test ./outputs/devdoctor.rb
```

Upgrade or reinstall a locally generated build:

```bash
brew reinstall --formula ./outputs/devdoctor.rb
```

Uninstall it:

```bash
brew uninstall devdoctor
```

The generated local formula uses a `file://` source URL and the real archive checksum, so it is installable without a remote repository. For a public tap, upload `devdoctor-<version>.tar.gz` to a stable HTTPS release URL, substitute that URL in the generated formula without changing the checksum, and place the formula at `Formula/devdoctor.rb` in a tap repository. Consumers can then run:

```bash
brew tap OWNER/devdoctor
brew install devdoctor
```

The local Intel-macOS release bundles a minimized Java 21 runtime built with `jlink`, installs the runtime and shaded JAR under `libexec`, and exposes a stable `devdoctor` launcher under Homebrew's `bin` directory. It therefore does not depend on a system JDK, Xcode, or another Homebrew formula. A public tap should publish separately built and tested Intel and Apple Silicon archives.

The locally generated formula intentionally uses `file://` URLs for both its archive and homepage. Homebrew installs and tests this form, but `brew style` will retain a homepage warning until the project has a real public HTTPS repository. Do not replace that field with an invented URL; set it to the repository homepage when publishing the tap.
