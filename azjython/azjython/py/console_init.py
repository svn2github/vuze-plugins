class InterpreterEnvironment:

    def printerror(self):
        import sys
        error_tpl = sys.exc_info()
        error_type = error_tpl[0]
        error = error_tpl[1]
    
        from java.lang import Throwable
        if isinstance(error, Throwable):
            error.printStackTrace(self.jstderr)
            return
        
        try:
            import traceback
        except ImportError:
            pass
        else:
            # Strip the newline characters at the end. We have to use format_exception
            # rather than print_exc, because we need to force the lines to go through
            # jstderr (rather than "sys.stderr" which will default to the multisource
            # streams).
            for line in traceback.format_exception(*error_tpl):
                self.jstderr.println(line.rstrip())
            return
        
        # If we don't have the traceback module available, just print it out
        # to stderr ourselves.
    
        # Indicates that it might be a string exception.
        if error is None:
            error_to_print = error_type
        else:
            error_to_print = "%s: %s" % (error_type, error)
    
        print >> self.jstderr, "ERROR: %s" % error_to_print
        
    def selected(self):
        return list(env._ui_namespace.getSelectedItems())
    
env = InterpreterEnvironment()
env.jstdout = jstdout
env.jstderr = jstderr
env.plugin_interface = env.pi = plugin_interface
env.swt_ui = swt_ui
env.config = plugin_interface.getPluginconfig()
env._ui_namespace = _ui_namespace

del jstdout, jstderr, plugin_interface, swt_ui, _ui_namespace, InterpreterEnvironment

# Quick access to classes in the plugin API.
from org.gudy.azureus2 import plugins as api
    
def azjython_help():
    trans = env.plugin_interface.getUtilities().getLocaleUtilities().localise
    def msg(suffix, msgno=None, trans=trans):
        if msgno:
            suffix += '.' + str(msgno)
        return trans('azjython.interpreter.help.' + suffix)

    print
    print '-------'
    for title_group, var_list in [
        ('env', ['pi', 'swt_ui', 'config', 'selected', 'printerror', 'jstdout', 'jstderr']),
        ('globals', ['api']),        
    ]:
        print
        print msg('title.' + title_group)
        for item in var_list:
            print '  %s:' % item, msg('name.' + item)
            i = 2
            while True:
                next_line = msg('name.' + item, msgno=i)
                if not next_line: break
                print ' ' * (3 + len(item)), next_line
                i += 1
        print
        print '-------'
    print
