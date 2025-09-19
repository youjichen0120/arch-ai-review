package cn.hexinfo.arch_ai_review.service.prompt;

import org.springframework.stereotype.Component;

@Component
public class ContentReviewPromptTemplate {
    
    private static final String TEMPLATE = """
        你是一个资深的软件架构师和文档评审专家。请对以下软件概要设计文档进行深度内容评审：
        
        文档标题：$document_title$
        文档内容：$document_content$
        
        请从以下维度进行评审：
        1. 设计合理性：架构设计是否合理，技术选型是否恰当
        2. 内容完整性：是否包含所有必要的设计要素
        3. 逻辑一致性：各部分内容是否逻辑一致，无矛盾
        4. 技术可行性：技术方案是否具备可实施性
        5. 安全性考虑：是否考虑了安全相关因素
        6. 性能考虑：是否考虑了性能相关因素
        7. 可维护性：设计是否便于后续维护和扩展
        
        你必须严格按照以下JSON格式返回评审结果，不要添加任何额外的文本、注释或Markdown格式。
        
        注意：下面只是格式示例，你需要根据实际文档内容生成评审结果，不要直接复制示例内容。
        
        {
            "score": 0.0,  // 根据文档质量评分，范围0-100
            "status": "",  // 必须是PASS、FAIL或NEED_REVIEW三者之一
            "issues": [
                // 这里列出实际发现的问题，不要使用示例问题
                {
                    "type": "",  // 必须是CONTENT、LOGIC或COMPLETENESS三者之一
                    "severity": "",  // 必须是HIGH、MEDIUM或LOW三者之一
                    "title": "",  // 问题的简短描述
                    "description": "",  // 详细说明问题
                    "suggestion": "",  // 改进建议
                    "pageNumber": 0,  // 如果能确定问题所在页码，否则可以省略
                    "lineNumber": 0  // 如果能确定问题所在行号，否则可以省略
                }
                // 如有多个问题，请添加多个问题项
            ],
            "summary": "",  // 总结性评价，不要使用示例文本
            "strengths": ["", ""],  // 文档的优点，不要使用示例文本
            "weaknesses": ["", ""],  // 文档的不足，不要使用示例文本
            "recommendations": ["", ""]  // 改进建议，不要使用示例文本
        }
        
        重要规则：
        1. 必须根据实际文档内容进行评审，不要返回示例或模板内容
        2. 评分必须在0-100范围内，根据文档质量客观评分
        3. 你的回复必须是一个有效的JSON对象，不包含任何其他内容
        4. 不要在JSON前后添加任何其他文本，如反引号、注释等
        5. 不要使用Markdown代码块格式
        6. 直接返回原始JSON，不要进行任何额外的格式化或解释
        7. 移除JSON中的所有注释，上面的注释仅供你理解
        """;
    
    public String format(String documentTitle, String documentContent) {
        return TEMPLATE
            .replace("$document_title$", documentTitle)
            .replace("$document_content$", documentContent);
    }
}
