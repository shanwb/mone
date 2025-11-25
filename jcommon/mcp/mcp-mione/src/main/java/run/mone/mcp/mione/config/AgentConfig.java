package run.mone.mcp.mione.config;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import run.mone.hive.mcp.function.ChatFunction;
import run.mone.hive.mcp.service.RoleMeta;
import run.mone.hive.roles.tool.AskTool;
import run.mone.hive.roles.tool.AttemptCompletionTool;
import run.mone.hive.roles.tool.ChatTool;

/**
 * @author shanwb
 * @date 2025/11/25
 */
@Configuration
public class AgentConfig {

    @Value("${mcp.agent.mode:AGENT}")
    private String agentMode;

    @Bean
    public RoleMeta roleMeta() {
        return RoleMeta.builder()
                .profile("你是一名优秀的MiOne助手")
                .goal("你的目标是更好的帮助用户")
                .constraints("专注于解决MiOne问题")
                //内部工具
                .tools(Lists.newArrayList(
                                new ChatTool(),
                                new AskTool(),
                                new AttemptCompletionTool()
                        )
                )
                .mode(RoleMeta.RoleMode.valueOf(agentMode))
                .mcpTools(
                    RoleMeta.RoleMode.valueOf(agentMode).equals(RoleMeta.RoleMode.AGENT) 
                        ? Lists.newArrayList(new ChatFunction("miline-new", 20)) 
                        : Lists.newArrayList(new ChatFunction("miline-new", 20))
                )
                .workflow("""
                    你是智能化系统，严格按照以下步骤执行：
                        - 根据projectName生成项目
                        - 根据提供的projectId、env生成代码,
                        - 拉取代码到本地
                        - 修改service的pom文件，不要排除spring-boot-starter-tomcat这个包
                        - 根据需求进行代码修改，检查下没有语法bug在进行后续提交操作
                        - 完成后，将本地代码使用git_commit工具进行git commit，commit信息是自动代码修复, 使用git_push进行git push
                        - 根据projectId、pipelineName、gitUrl、gitName创建流水线
                        - 根据projectId、pipelineId触发流水线进行发布
                """)
                .build();
    }

}


