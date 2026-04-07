package ai.binbun.cli;

import ai.logicbean.tui.Component;
import ai.logicbean.tui.components.Box;
import ai.logicbean.tui.components.InputField;
import ai.logicbean.tui.components.ScrollView;
import ai.logicbean.tui.components.Text;
import ai.logicbean.tui.layout.FlexDirection;
import ai.logicbean.tui.layout.JustifyContent;

public final class AgentLayout {

    private final Box root;
    private final ScrollView messageView;
    private final InputField inputField;
    private final Text statusBar;

    public AgentLayout() {
        this.root = Box.create()
                .flexDirection(FlexDirection.COLUMN)
                .justifyContent(JustifyContent.SPACE_BETWEEN)
                .fill();

        this.messageView = ScrollView.create()
                .flexGrow(1)
                .fillWidth();

        this.inputField = InputField.create()
                .placeholder("Enter prompt... (Ctrl+C to exit)")
                .fillWidth();

        this.statusBar = Text.create("Ready")
                .dimmed()
                .padding(0, 1);

        root.addChild(messageView);
        root.addChild(inputField);
        root.addChild(statusBar);
    }

    public Component root() {
        return root;
    }

    public ScrollView messageView() {
        return messageView;
    }

    public InputField inputField() {
        return inputField;
    }

    public Text statusBar() {
        return statusBar;
    }

    public void appendText(String delta) {
        var last = messageView.lastChild();
        if (last instanceof Text text) {
            text.append(delta);
        } else {
            messageView.addChild(Text.create(delta));
        }
        messageView.scrollToEnd();
    }

    public void commitMessage(String role, String content) {
        var message = Box.create()
                .padding(0, 1)
                .flexDirection(FlexDirection.COLUMN);

        var header = Text.create(role.substring(0, 1).toUpperCase() + role.substring(1) + ":")
                .bold()
                .foreground(role.equals("assistant") ? 0x42a5f5 : 0x66bb6a);

        var body = Text.create(content)
                .wrap();

        message.addChild(header);
        message.addChild(body);
        message.addChild(Text.create(""));

        messageView.addChild(message);
        messageView.scrollToEnd();
    }
}
