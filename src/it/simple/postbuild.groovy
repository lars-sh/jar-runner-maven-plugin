println 'Assert build log exists'
File buildLog = new File(basedir, 'build.log')
assert buildLog.exists()

println 'Assert build log was successful'
assert buildLog.text.contains("Usage: checkstyle")
assert buildLog.text.contains("--version")
