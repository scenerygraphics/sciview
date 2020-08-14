import subprocess
import os

#script_path = os.path.dirname(os.path.realpath(__file__))
script_path = os.path.abspath( os.path.dirname(sys.argv[0]) )

classpath = '%s/jars/*' % script_path
classpath += ':%s/jars/bio-formats/*' % script_path
classpath += ':%s/jars/linux32/*' % script_path
classpath += ':%s/jars/linux64/*' % script_path
classpath += ':%s/jars/macosx/*' % script_path
classpath += ':%s/jars/win32/*' % script_path
classpath += ':%s/jars/win64/*' % script_path

print([script_path, classpath])

subprocess.call(['java', '-cp', classpath, 'sc.iview.Main'])
