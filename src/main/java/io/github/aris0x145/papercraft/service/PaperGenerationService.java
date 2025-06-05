package io.github.aris0x145.papercraft.service;

import io.github.aris0x145.papercraft.model.PaperRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * 論文生成服務
 */
@Slf4j
@Service
public class PaperGenerationService {

    private final ChatClient plannerAgent;
    private final ChatClient writerAgent;
    private final ChatClient editorAgent;

    /**
     * 構造函數 - 注入三個 Agent 的 ChatClient
     */
    public PaperGenerationService(
            @Qualifier("plannerAgentChatClient") ChatClient plannerAgent,
            @Qualifier("writerAgentChatClient") ChatClient writerAgent,
            @Qualifier("editorAgentChatClient") ChatClient editorAgent) {
        this.plannerAgent = plannerAgent;
        this.writerAgent = writerAgent;
        this.editorAgent = editorAgent;
    }

    /**
     * 流式生成論文
     */
    public Flux<String> generatePaperStream(PaperRequest request) {
        return generatePaperStream(request, null);
    }

    /**
     * 流式生成論文
     * 
     * @param request 論文生成請求
     * @param fileContents 上傳文件的內容列表（可選）
     */
    public Flux<String> generatePaperStream(PaperRequest request, List<String> fileContents) {
        String conversationId = "paper-" + UUID.randomUUID();
        log.info("Starting paper generation with conversationId: {}", conversationId);
        
        return Flux.concat(
            Flux.just("=== 開始生成論文大綱 ===\n\n")
                .concatWith(generateOutlineStream(request, fileContents, conversationId))
                .concatWith(Flux.just("\n\n=== 大綱生成完成 ===\n\n")),

            Flux.just("=== 開始撰寫論文內容 ===\n\n")
                .concatWith(writeContentStream(request, fileContents, conversationId))
                .concatWith(Flux.just("\n\n=== 內容撰寫完成 ===\n\n")),

            Flux.just("=== 開始編輯潤色 ===\n\n")
                .concatWith(editContentStream(request, conversationId))
                .concatWith(Flux.just("\n\n=== 論文生成完成 ===\n\n"))
        )
        .doOnError(error -> log.error("Error during paper generation for conversationId: {}", conversationId, error))
        .doOnTerminate(() -> log.info("Paper generation completed for conversationId: {}", conversationId));
    }

    /**
     * 流式生成大綱
     */
    private Flux<String> generateOutlineStream(PaperRequest request, List<String> fileContents, String conversationId) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append(String.format("""
            你是一位專業的學術規劃專家 (PlannerAgent)，專精於APA第七版格式的學術論文設計。
            請為以下論文需求生成詳細且結構化的大綱：
           
            📚 論文基本信息：
            • 研究主題: %s
            • 論文類型: %s
            • 學術領域: %s
            • 輸出語言: %s
            • 特殊需求: %s
           """,
            request.getTopic(),
            request.getType(),
            request.getField(),
            request.getLanguage(),
            request.getRequirements() != null ? request.getRequirements() : "無特殊需求"
        ));

        // 如果有文件內容，加入到提示中
        if (fileContents != null && !fileContents.isEmpty()) {
            promptBuilder.append("\n\n📄 參考文件內容分析：\n");
            for (int i = 0; i < fileContents.size(); i++) {
                promptBuilder.append(String.format("=== 文件 %d ===\n%s\n\n", i + 1, fileContents.get(i)));
            }
            promptBuilder.append("**重要指示**: 請深度分析上述文件內容，提取關鍵數據、理論框架、研究發現和論證要點，並系統性地整合到以下大綱結構中。");
        }

        promptBuilder.append(String.format("""
            
            🎯 請生成符合APA第七版格式的完整學術論文大綱，使用%s撰寫：
            
            ## 📋 論文結構大綱 (APA第七版標準)
            
            ### 1. 封面頁 (Title Page)
            • 論文標題：[具體、簡潔、學術性強，不超過12個詞]
            • 作者資訊：[作者姓名、所屬機構、課程名稱、指導教授、提交日期]
            • 格式要求：[居中對齊、雙行間距、Times New Roman 12pt字體]
            
            ### 2. 摘要 (Abstract) - 150-250字
            **框架結構：**
            • 研究背景與問題陳述 (30-40字)
            • 研究目的與假設 (20-30字)
            • 研究方法與設計 (40-50字)
            • 主要研究結果 (40-60字)
            • 結論與意義 (20-30字)
            **關鍵詞：** [5-8個學術關鍵詞，按字母順序排列]
            
            ### 3. 目錄 (Table of Contents)
            • 各章節標題及對應頁碼
            • 圖表清單 (如適用)
            • 附錄清單 (如適用)
            
            ### 4. 緒論/引言 (Introduction)
            **內容要點：**
            • 研究背景與重要性說明
            • 文獻回顧與理論基礎概述
            • 研究問題的明確陳述
            • 研究目的與假設
            • 研究貢獻與創新點
            • 論文結構概述
            **撰寫重點：** [具體說明如何建立研究問題的學術價值]
            
            ### 5. 文獻探討 (Literature Review)
            **內容要點：**
            • 相關理論框架的系統性回顧
            • 關鍵研究成果的批判性分析
            • 研究方法論的比較與評估
            • 現有研究的局限性與缺口識別
            • 本研究的理論定位與貢獻
            **撰寫重點：** [展現批判思維，避免單純羅列文獻]
            
            ### 6. 研究方法 (Methodology)
            **內容要點：**
            • 研究設計與研究哲學
            • 研究對象與抽樣方法
            • 資料收集工具與程序
            • 資料分析方法與技術
            • 研究倫理考量
            • 研究信度與效度確保
            **撰寫重點：** [確保研究的可重複性與嚴謹性]
            
            ### 7. 結果分析 (Results)
            **內容要點：**
            • 描述性統計結果呈現
            • 假設檢驗與推論統計
            • 圖表數據的專業解讀
            • 主要發現的客觀陳述
            • 意外發現或異常數據說明
            **撰寫重點：** [客觀呈現，避免主觀解釋]
            
            ### 8. 討論 (Discussion)
            **內容要點：**
            • 研究結果的深度解釋與意義
            • 與既有文獻的比較分析
            • 理論貢獻與實務意涵
            • 研究局限性的誠實討論
            • 未來研究方向的建議
            • 研究的社會價值與影響
            **撰寫重點：** [展現批判性思維與學術洞察力]
            
            ### 9. 結論 (Conclusion)
            **內容要點：**
            • 研究問題的回答與總結
            • 主要研究貢獻的重申
            • 理論與實務建議
            • 研究局限性的最終反思
            • 未來研究的具體方向
            **撰寫重點：** [簡潔有力，呼應緒論]
            
            ### 10. 參考文獻 (References)
            **APA第七版格式要求：**
            • 按作者姓氏字母順序排列
            • 懸掛縮排格式
            • 正確的期刊、書籍、網路資源引用格式
            • 建議包含20-50篇高質量學術文獻
            
            ### 11. 附錄 (Appendices) - 如適用
            **可能包含：**
            • 問卷調查工具 (Appendix A)
            • 原始數據表格 (Appendix B)
            • 補充分析結果 (Appendix C)
            • 相關圖表或資料 (Appendix D)
            
            ## 📐 格式規範要求：
            • **字體：** Times New Roman 12pt (內文)、Arial 12pt (圖表標題)
            • **行距：** 雙行間距 (2.0)
            • **邊界：** 上下左右各2.54cm (1英寸)
            • **頁碼：** 右上角，從摘要頁開始編號
            • **標題層級：** 遵循APA第七版五級標題系統
            • **引用格式：** 嚴格遵循APA第七版內文引用與參考文獻格式
            
            ## 💡 大綱品質標準：
            • 邏輯結構嚴密，章節間具有清晰的因果關係
            • 每個章節都有具體的內容指導和撰寫重點
            • 提供充分的細節指引，確保後續WriterAgent能夠精確展開
            • 如有參考文件，明確標註在各章節中的具體運用方式
            • 完全符合%s領域的學術標準和APA第七版規範
            • 確保研究問題的學術價值和創新性
            
            請立即開始生成詳細的結構化大綱：
            """, 
            request.getLanguage(), 
            request.getField()
        ));
        
        return plannerAgent
            .prompt()
            .user(promptBuilder.toString())
            .advisors(advisorSpec -> advisorSpec.param("CONVERSATION_ID", conversationId))
            .stream()
            .content();
    }

    /**
     * 流式撰寫內容（支援文件內容）- 基於APA第七版格式標準
     */
    private Flux<String> writeContentStream(PaperRequest request, List<String> fileContents, String conversationId) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append(String.format("""
            你是一位專業的學術寫作專家 (WriterAgent)，精通APA第七版格式的學術論文撰寫。
            請為論文主題"%s"撰寫高質量的學術內容。
            
            📚 論文詳細信息：
            • 研究主題: %s
            • 論文類型: %s
            • 研究領域: %s
            • 輸出語言: %s
            • 特殊需求: %s
            """,
            request.getTopic(),
            request.getTopic(),
            request.getType(),
            request.getField(),
            request.getLanguage(),
            request.getRequirements() != null ? request.getRequirements() : "無特殊需求"
        ));

        // 如果有文件內容，提醒使用這些內容
        if (fileContents != null && !fileContents.isEmpty()) {
            promptBuilder.append("\n\n📄 參考文件資源整合指引：\n");
            promptBuilder.append("用戶已提供相關研究資料，PlannerAgent在制定大綱時已充分考慮這些內容。\n");
            promptBuilder.append("在撰寫過程中，請專業地整合以下資訊類型：\n");
            promptBuilder.append("• **實證數據與統計分析** - 用於支持論證和假設檢驗\n");
            promptBuilder.append("• **案例研究與實際應用** - 增強理論的實務連結性\n"); 
            promptBuilder.append("• **理論框架與概念模型** - 建立堅實的學術基礎\n");
            promptBuilder.append("• **研究方法與分析技術** - 確保方法論的嚴謹性\n");
            promptBuilder.append("• **文獻資源與學術觀點** - 支持批判性討論與分析\n");
            promptBuilder.append("• **創新發現與洞察** - 突出研究的原創性貢獻\n\n");
            promptBuilder.append("**整合原則**: 精確引用、適當歸屬、邏輯連結、價值增值\n");
        }

        promptBuilder.append(String.format("""
            
            ## 🎯 核心撰寫任務：
            **嚴格遵循PlannerAgent生成的APA格式大綱結構**，對每個章節進行學術性的深度展開。
            
            ## 📝 APA第七版撰寫標準：
            
            ### 🏗️ 結構完整性要求：
            1. **嚴格按照大綱的章節順序和編號系統撰寫**
            2. **確保每個章節達到大綱建議的字數要求**
            3. **深度展開大綱中的每個核心要點和子要點**
            4. **維持學術寫作的客觀性、嚴謹性和專業性**
            5. **適當整合參考文件內容，並明確標註來源**
            6. **確保內容的學術深度符合研究生/博士生水準**
            
            ### 📋 各章節具體撰寫指南：
            
            #### 1. **封面頁 (Title Page)**
            • 論文標題：簡潔明確，體現研究核心（不超過12個詞）
            • 作者資訊：完整的學術身份標識
            • 格式：嚴格遵循APA第七版封面頁格式
            
            #### 2. **摘要 (Abstract) - 150-250字**
            • **研究背景** (2-3句)：簡要介紹研究領域和問題重要性
            • **研究目的** (1-2句)：明確陳述研究目標和假設
            • **研究方法** (2-3句)：概述研究設計、樣本和分析方法
            • **主要結果** (3-4句)：重點呈現關鍵發現和數據
            • **結論意義** (1-2句)：總結研究貢獻和實務意涵
            • **關鍵詞**：5-8個專業術語，按字母順序排列
            
            #### 3. **緒論/引言 (Introduction)**
            • **研究背景設定**：建立研究問題的學術和實務重要性
            • **文獻回顧概述**：簡要回顧相關理論基礎和關鍵研究
            • **問題陳述**：清晰識別研究缺口和待解決問題
            • **研究目的與假設**：明確陳述研究目標和預期發現
            • **研究貢獻預覽**：說明研究的創新性和學術價值
            • **論文結構指引**：簡要介紹各章節內容安排
            
            #### 4. **文獻探討 (Literature Review)**
            • **理論框架建構**：系統性介紹相關理論基礎
            • **實證研究回顧**：批判性分析既有研究成果
            • **方法論比較**：評估不同研究方法的優缺點
            • **研究缺口識別**：明確指出現有研究的局限性
            • **假設發展**：基於文獻回顧建立研究假設
            • **理論貢獻定位**：說明本研究的理論創新點
            
            #### 5. **研究方法 (Methodology)**
            • **研究設計說明**：詳述研究哲學和方法論選擇
            • **樣本設計**：描述研究對象、抽樣方法和樣本特徵
            • **資料收集**：詳細說明資料收集工具和程序
            • **變數操作定義**：明確定義所有研究變數
            • **分析方法**：說明統計分析技術和軟體使用
            • **信效度確保**：討論研究工具的可靠性和有效性
            • **倫理考量**：說明研究倫理的保障措施
            
            #### 6. **結果分析 (Results)**
            • **描述性統計**：詳細呈現樣本特徵和變數分布
            • **推論統計**：系統報告假設檢驗結果
            • **圖表解讀**：專業解釋統計圖表和數據模式
            • **主要發現陳述**：客觀報告關鍵研究結果
            • **額外分析**：報告補充分析和敏感性檢驗
            • **結果總結**：簡潔總結主要統計發現
            
            #### 7. **討論 (Discussion)**
            • **結果解釋**：深度詮釋研究發現的學術意義
            • **理論對話**：將結果與既有理論進行對話
            • **實務意涵**：說明研究發現的實際應用價值
            • **比較分析**：與相關研究進行比較和對比
            • **局限性討論**：誠實分析研究的限制和不足
            • **未來方向**：提出具體的後續研究建議
            
            #### 8. **結論 (Conclusion)**
            • **研究問題回答**：直接回應研究問題和假設
            • **核心貢獻總結**：簡潔陳述主要學術和實務貢獻
            • **理論意義**：強調研究對理論發展的推進作用
            • **實務建議**：提出具體的管理或政策建議
            • **研究局限反思**：最終反思研究的整體限制
            • **未來研究呼籲**：鼓勵學術社群的進一步探索
            
            ### ⚠️ 品質控制要求：
            • **完整性保證**：每個章節都必須有實質性的詳細內容
            • **深度標準**：內容深度符合%s領域的研究生學術水準
            • **語言品質**：使用正確的%s學術寫作風格和專業術語
            • **邏輯連貫**：確保論證邏輯的嚴密性和章節間的連接性
            • **格式遵循**：嚴格遵循APA第七版的所有格式要求
            • **引用規範**：正確使用APA格式的內文引用 (Author, Year)
            • **創新展現**：明確展示研究的原創性和學術價值
            • **完整呈現**：避免任何章節內容的截斷或不完整
            
            ### 🎯 最終品質標準：
            論文必須達到能夠提交至國際期刊審查的學術水準，展現：
            • 紮實的理論基礎和文獻掌握能力
            • 嚴謹的研究方法和分析技能
            • 創新的學術洞察和批判思維
            • 清晰的學術表達和邏輯論證
            • 完整的研究倫理和學術誠信
            
            🔥 請立即開始撰寫完整且高質量的學術論文內容：
            """, 
            request.getField(),
            request.getLanguage()
        ));
        
        return writerAgent
            .prompt()
            .user(promptBuilder.toString())
            .advisors(advisorSpec -> advisorSpec.param("CONVERSATION_ID", conversationId))
            .stream()
            .content();
    }

    /**
     * 流式編輯潤色 - APA第七版格式專業編輯
     */
    private Flux<String> editContentStream(PaperRequest request, String conversationId) {
        String prompt = String.format("""
            你是一位國際頂尖的學術編輯專家 (EditorAgent)，專精於APA第七版格式的學術論文編輯與品質控制。
            請對前面生成的論文進行全面的專業編輯和精緻潤色，確保達到國際期刊發表水準。
            
            📚 論文基本資訊：
            • 研究主題: %s
            • 論文類型: %s
            • 學術領域: %s
            • 輸出語言: %s
            • 特殊需求: %s
            
            ## 🎯 編輯任務總覽：
            基於PlannerAgent的結構化大綱和WriterAgent的學術內容，進行系統性的編輯優化，
            確保論文達到可直接提交至SSCI/SCI期刊的專業水準。
            
            ## 📝 全方位編輯檢核標準：
            
            ### 1. 🏗️ 結構完整性與邏輯檢核
            #### **架構完整性驗證：**
            • ✅ 檢查所有APA標準章節是否完整包含（封面頁→摘要→目錄→緒論→文獻探討→方法→結果→討論→結論→參考文獻）
            • ✅ 驗證章節間的邏輯流暢性和因果關係連結
            • ✅ 確認每個章節都完整回應了大綱要求
            • ⚠️ **關鍵任務**：如發現WriterAgent內容不完整或被截斷，必須補充完成
            
            #### **論證邏輯強化：**
            • 增強研究問題與假設的邏輯連結
            • 確保文獻回顧與研究方法的理論一致性
            • 驗證結果分析與討論部分的因果邏輯
            • 強化緒論與結論的首尾呼應關係
            
            ### 2. 📊 內容品質深度提升
            #### **學術深度優化：**
            • **理論貢獻強化**：明確突出研究的理論創新點和學術價值
            • **方法論嚴謹性**：確保研究方法的科學性和可重複性
            • **實證分析深化**：增強統計分析的專業性和解釋力
            • **批判思維展現**：在文獻回顧和討論中加強批判性分析
            • **創新性彰顯**：清晰標示研究的原創性貢獻和突破點
            
            #### **內容完整性確保：**
            • **補充關鍵細節**：增加必要的概念定義、理論解釋和實證支持
            • **數據呈現優化**：改善統計結果的表達和圖表解讀
            • **案例分析深化**：如有案例，確保分析的深度和洞察力
            • **實務意涵強化**：增強研究發現對實務應用的指導價值
            • **完整性檢查**：確保每個章節都有充分的內容深度
            
            ### 3. ✏️ APA第七版格式精準校正
            #### **標題系統規範化：**
            • **五級標題格式**：嚴格遵循APA第七版標題層級系統
            • **編號系統統一**：確保章節編號的一致性和邏輯性
            • **標題表達優化**：使標題更具學術性和吸引力
            
            #### **引用格式標準化：**
            • **內文引用**：確保所有引用都符合(Author, Year)或(Author, Year, p. #)格式
            • **多作者處理**：正確處理2-6位作者和6位以上作者的引用格式
            • **引用整合**：適當增加高質量文獻引用以增強學術權威性
            • **引用平衡**：確保引用文獻的時效性和權威性平衡
            
            #### **參考文獻完善：**
            • **格式統一**：所有參考文獻嚴格遵循APA第七版格式
            • **資訊完整**：確保每個參考文獻資訊的完整性和準確性
            • **品質控制**：優先選擇高影響因子期刊和權威出版社文獻
            • **數量適中**：建議維持20-50篇高質量參考文獻
            
            ### 4. 🎨 語言風格與表達優化
            #### **學術語言精煉：**
            • **專業術語統一**：確保專業概念和術語使用的一致性
            • **句式結構優化**：改善複雜句的清晰度和可讀性
            • **語氣客觀化**：維持學術寫作的客觀性和中性立場
            • **表達簡潔化**：消除冗餘表達，提升語言效率
            
            #### **%s語言品質提升：**
            • **語法精確性**：修正所有語法、拼寫和標點錯誤
            • **詞彙專業性**：使用精確的學術詞彙和領域術語
            • **表達流暢性**：確保整體閱讀的順暢性和邏輯性
            • **文化適切性**：確保表達方式符合國際學術慣例
            
            ### 5. 🔍 學術倫理與品質控制
            #### **學術誠信確保：**
            • **原創性檢核**：確保所有觀點和發現的原創性表達
            • **歸屬準確性**：正確歸屬所有引用的思想和數據來源
            • **客觀性維持**：保持研究立場的中立和客觀
            • **倫理合規性**：確保研究符合學術倫理標準
            
            #### **品質一致性控制：**
            • **風格統一**：確保整篇論文的寫作風格和語調一致
            • **術語一致**：統一所有專業術語和概念的使用
            • **格式一致**：所有圖表、表格、引用格式的統一性
            • **邏輯一致**：確保論證邏輯在整篇論文中的一致性
            
            ### 6. 🎯 特殊章節重點優化
            #### **摘要精煉 (150-250字)**：
            • 確保完整包含背景、目的、方法、結果、結論五要素
            • 語言簡潔精準，每句話都有明確目的
            • 關鍵詞選擇的準確性和檢索價值
            
            #### **結論章節強化**：
            • **完整性確保**：必須包含研究貢獻總結、實務建議、局限性反思、未來方向
            • **價值彰顯**：明確陳述研究的理論和實務價值
            • **前瞻性展望**：提出具體的後續研究建議
            • **簡潔有力**：用精煉的語言表達核心訊息
            
            #### **討論章節深化**：
            • **解釋深度**：對結果進行深層次的學術解釋
            • **比較分析**：與既有研究進行系統性比較
            • **意義闡述**：清晰說明研究的理論和實務意義
            • **局限誠實**：客觀討論研究的限制和不足
            
            ## ⚠️ 編輯品質控制要求：
            
            ### 🔥 核心任務優先級：
            1. **完整性第一**：確保提供完整的最終論文版本，絕不遺漏任何章節
            2. **品質至上**：每個章節都必須達到國際期刊發表水準
            3. **格式嚴謹**：嚴格遵循APA第七版所有格式要求
            4. **邏輯嚴密**：確保整篇論文的論證邏輯無懈可擊
            5. **創新突出**：明確展現研究的原創性和學術貢獻
            
            ### 📋 最終交付標準：
            編輯後的論文必須完整包含：
            ✅ **結構完整的標題頁**：符合APA格式的所有要素
            ✅ **精煉的摘要和關鍵詞**：150-250字，涵蓋五大要素
            ✅ **詳細的目錄**：清晰的章節結構和頁碼
            ✅ **充實的各主要章節**：每章節都有足夠的學術深度
            ✅ **完整的討論部分**：深度解釋、比較分析、意義闡述、局限討論
            ✅ **完整的結論章節**：貢獻總結、實務建議、局限反思、未來方向
            ✅ **規範的參考文獻**：嚴格的APA第七版格式
            ✅ **必要的附錄**：如有相關補充材料
            
            ### 🎖️ 卓越品質指標：
            • **理論深度**：展現扎實的理論基礎和創新洞察
            • **方法嚴謹**：體現高水準的研究設計和分析能力
            • **邏輯清晰**：論證結構嚴密，表達邏輯流暢
            • **格式完美**：完全符合APA第七版國際標準
            • **語言優雅**：學術表達精準、優雅、有力
            • **貢獻明確**：清晰展現研究的學術和實務價值
            
            ## 🚀 請立即開始全面編輯，提供達到國際期刊發表水準的完整論文：
            """, 
            request.getTopic(),
            request.getType(),
            request.getField(),
            request.getLanguage(),
            request.getRequirements() != null ? request.getRequirements() : "無特殊需求",
            request.getLanguage()
        );
        
        return editorAgent
            .prompt()
            .user(prompt)
            .advisors(advisorSpec -> advisorSpec.param("CONVERSATION_ID", conversationId))
            .stream()
            .content();
    }
}
