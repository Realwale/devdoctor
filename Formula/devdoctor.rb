# typed: strict
# frozen_string_literal: true

# Homebrew formula for the DevDoctor diagnostic CLI.
class Devdoctor < Formula
  desc "Evidence-based root-cause diagnostics for software failures"
  homepage "https://github.com/Realwale/devdoctor"
  url "https://github.com/Realwale/devdoctor/releases/download/v0.2.0/devdoctor-0.2.0-macos-x86_64.tar.gz"
  version "0.2.0"
  sha256 "ef051a943019f5bfcceab63d1f0cc4170ba231ed449b5ae7deb7bbd7f8213d75"
  license "Apache-2.0"

  depends_on :macos

  def install
    raise "This release was built for Intel Macs" unless Hardware::CPU.intel?

    libexec.install "devdoctor.jar", "devdoctor-agent.jar", "devdoctor-control-agent.jar", "runtime"

    java = libexec/"runtime/bin/java"
    (bin/"devdoctor").write <<~SH
      #!/bin/bash
      exec "#{java}" -jar "#{libexec}/devdoctor.jar" "$@"
    SH
  end

  test do
    assert_match "DevDoctor 0.2.0", shell_output("#{bin}/devdoctor version")

    (testpath/"application.log").write <<~LOG
      2026-08-17T00:00:00Z INFO [main] Application started successfully
    LOG
    command = "#{bin}/devdoctor diagnose --log #{testpath}/application.log " \
              "--project #{testpath} --offline --no-save"
    output = shell_output(command)
    assert_match "NO FAILURE REPRODUCED", output
  end
end
