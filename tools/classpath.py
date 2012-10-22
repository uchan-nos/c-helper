import os
import sys
import re

if __name__ == '__main__':
    plugindir = '/Applications/eclipse-juno/plugins'
    if len(sys.argv) == 2:
        plugindir = sys.argv[1]
    elif len(sys.argv) != 1:
        print('Usage: python classpath.py [plugin-directory]')
        print('Example:')
        print('  export CLASSPATH=`python classpath.py`')
    classpath = '.'
    jarpat = re.compile(r'.*\.jar')
    files = os.listdir(plugindir)
    for f in files:
        m = jarpat.match(f)
        if m:
            classpath += ':' + os.path.join(plugindir, f)
    print(classpath)

