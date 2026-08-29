# typed: strict
# frozen_string_literal: true

# Homebrew formula for the DevDoctor diagnostic CLI.
class Devdoctor < Formula
  desc "Evidence-based root-cause diagnostics for software failures"
  homepage "https://github.com/Realwale/devdoctor"
  url "https://github.com/Realwale/devdoctor/releases/download/v0.1.2/devdoctor-0.1.2-macos-x86_64.tar.gz"
  version "0.1.2"
  sha256 "b561f8946ecf13b77d3810f624abdd37804fd85a45e3b2a2904deb0585976834"
  license "Apache-2.0"

  depends_on :macos

  def install
    raise "This release was built for Intel Macs" unless Hardware::CPU.intel?

    libexec.install "devdoctor.jar", "runtime"

    java = libexec/"runtime/bin/java"
    (bin/"devdoctor").write <<~SH
      #!/bin/bash
      exec "#{java}" -jar "#{libexec}/devdoctor.jar" "$@"
    SH
  end

  test do
    assert_match "DevDoctor 0.1.2", shell_output("#{bin}/devdoctor version")

    (testpath/"application.log").write <<~LOG
      2026-08-17T00:00:00Z INFO [main] Application started successfully
    LOG
    command = "#{bin}/devdoctor diagnose --log #{testpath}/application.log " \
              "--project #{testpath} --offline --no-save"
    output = shell_output(command)
    assert_match "NO FAILURE REPRODUCED", output
  end
end
