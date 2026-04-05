package ai.binbun.skills;

import ai.binbun.tools.ToolRegistry;

public interface RuntimeSkill {
    String name();
    SkillActivation activate(SkillExecutionContext context, ToolRegistry tools);
}
