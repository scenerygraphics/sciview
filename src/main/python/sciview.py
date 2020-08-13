import subprocess
import os

script_path = os.path.dirname(os.path.realpath(__file__))
subprocess.call(['java', '-cp', '%s/jars/*' % script_path, 'sc.iview.Main'])
