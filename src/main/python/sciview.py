import subprocess
import os
import sys

# https://stackoverflow.com/questions/7674790/bundling-data-files-with-pyinstaller-onefile
def resource_path(relative_path):
    """ Get absolute path to resource, works for dev and for PyInstaller """
    try:
        # PyInstaller creates a temp folder and stores path in _MEIPASS
        base_path = sys._MEIPASS
    except Exception:
        base_path = os.path.abspath(".")

    return os.path.join(base_path, relative_path)

classpath = resource_path('./jars/*')
classpath += ':%s' % resource_path( './jars/bio-formats/*' )
classpath += ':%s' % resource_path( './jars/linux32/*' )
classpath += ':%s' % resource_path( './jars/linux64/*' )
classpath += ':%s' % resource_path( './jars/macosx/*' )
classpath += ':%s' % resource_path( './jars/win32/*' )
classpath += ':%s' % resource_path( './jars/win64/*' )

subprocess.call(['java', '-cp', classpath, '-DsciviewStandalone=true', 'sc.iview.Main'])
