//I used the following source to find the way to attach the debugger. I guessed with looping over the available debuggers, maybe there is a better way?
// https://github.com/JetBrains/intellij-community/blob/3a3cbc92d69d1190e50b57b7711cf5b2fe622d80/platform/xdebugger-impl/src/com/intellij/xdebugger/impl/actions/AttachToLocalProcessAction.java

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.xdebugger.attach.XLocalAttachDebugger;
import com.intellij.xdebugger.attach.XLocalAttachDebuggerProvider;

import java.util.List;

public class AutoAttachAction extends com.intellij.openapi.actionSystem.AnAction {
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
       Editor editor =  anActionEvent.getData(LangDataKeys.EDITOR);
       Project project =  editor.getProject();
       if(project == null){
           Messages.showDialog("It appears you don't have a project open.", "AutoAttach cancelled.", new String[]{"OK"}, -1, null);
            return;
       }

        ProcessInfo[] processList = OSProcessUtil.getProcessList();
        String projectName = project.getName();

        ProcessInfo selectedProcess = null;
        for (int i = 0; i < processList.length; i++) {
            ProcessInfo process = processList[i];
            if(process.getCommandLine().contains(projectName)){
                selectedProcess = process;
                break;
            }
        }

        if(selectedProcess == null)
        {
            Messages.showDialog("Could not find a Process containing " + projectName  +" to attach to.", "AutoAttach failed to attach.", new String[]{"OK"}, -1, null);
            return;
        }
        XLocalAttachDebuggerProvider[] providers = Extensions.getExtensions(XLocalAttachDebuggerProvider.EP);
        XLocalAttachDebugger debugger = null;

        for (int i = 0; i < providers.length; i++){
            XLocalAttachDebuggerProvider provider = providers[i];
            UserDataHolderBase dataHolder = new UserDataHolderBase();
            List<XLocalAttachDebugger> debuggers =  provider.getAvailableDebuggers(project,selectedProcess,dataHolder);
            if(debuggers.size() > 0) {
                debugger = debuggers.get(0);
                break;
            }
        }

        if(debugger == null)
        {
            Messages.showDialog("Could not find a compatible debugger for the process : " + selectedProcess.getPid() +  selectedProcess.getCommandLine() , "AutoAttach failed to attach.", new String[]{"OK"}, -1, null);
            return;
        }

        try {
            debugger.attachDebugSession(editor.getProject(),selectedProcess);
        }
        catch (ExecutionException e) {
            Messages.showDialog(" ExecutionException " + e.getMessage() + selectedProcess.getCommandLine(), "AutoAttach failed to attach.", new String[]{"OK"}, -1, null);
        }
    }
}
