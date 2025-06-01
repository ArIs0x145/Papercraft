// Papercraft JavaScript Application
let selectedFiles = [];

// 頁面初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('Papercraft 應用初始化...');
    
    // 初始化生成模式顯示
    updateGenerationMode();
    
    // 設置論文生成按鈕事件
    const generateBtn = document.getElementById('generateBtn');
    if (generateBtn) {
        generateBtn.addEventListener('click', handleGenerateClick);
        console.log('生成按鈕事件綁定成功');
    } else {
        console.error('找不到生成按鈕元素');
    }
    
    // 設置文件選擇事件
    const fileUpload = document.getElementById('fileUpload');
    if (fileUpload) {
        fileUpload.addEventListener('change', handleFileChange);
        console.log('文件上傳事件綁定成功');
    } else {
        console.error('找不到文件上傳元素');
    }
});

// 處理生成按鈕點擊
async function handleGenerateClick() {
    console.log('生成按鈕被點擊');
    
    try {
        // 獲取表單數據
        const formData = getFormData();
        
        // 驗證必填欄位
        if (!formData.topic || formData.topic.trim() === '') {
            showError('請先填寫論文主題');
            return;
        }
        
        console.log('表單數據:', formData);
        console.log('選擇的文件數量:', selectedFiles.length);
        
        // 根據是否有文件選擇生成模式
        if (selectedFiles.length > 0) {
            await generatePaper(formData, selectedFiles);
        } else {
            await generatePaper(formData);
        }
        
    } catch (error) {
        console.error('生成論文時發生錯誤:', error);
        showError('生成論文時發生錯誤: ' + error.message);
        resetForm();
    }
}

// 獲取表單數據
function getFormData() {
    return {
        topic: document.getElementById('topic')?.value || '',
        type: document.getElementById('type')?.value || '研究論文',
        field: document.getElementById('field')?.value || '',
        language: document.getElementById('language')?.value || '繁體中文',
        requirements: document.getElementById('requirements')?.value || ''
    };
}

// 處理文件選擇變更
function handleFileChange(event) {
    selectedFiles = Array.from(event.target.files);
    console.log('選擇的文件:', selectedFiles.map(f => f.name));
    updateFileList();
    updateGenerationMode();
}

// 更新生成模式顯示
function updateGenerationMode() {
    const modeDiv = document.getElementById('generationMode');
    const modeText = document.getElementById('modeText');
    const btnIcon = document.getElementById('btnIcon');
    const btnText = document.getElementById('btnText');
    
    // 防護措施：確保所有元素都存在
    if (!modeDiv || !modeText || !btnIcon || !btnText) {
        console.warn('updateGenerationMode: 某些 DOM 元素未找到');
        return;
    }
    
    if (selectedFiles.length > 0) {
        modeDiv.style.display = 'block';
        modeText.textContent = `📄 將基於 ${selectedFiles.length} 個上傳文件生成論文`;
        btnIcon.textContent = '📄';
        btnText.textContent = '基於文件生成論文';
    } else {
        modeDiv.style.display = 'block';
        modeText.textContent = '⚡ 將使用流式生成模式（僅基於您的輸入）';
        btnIcon.textContent = '⚡';
        btnText.textContent = '流式生成論文';
    }
}

// 更新文件列表顯示
function updateFileList() {
    const fileListDiv = document.getElementById('fileList');
    if (!fileListDiv) return;
    
    fileListDiv.innerHTML = '';

    if (selectedFiles.length === 0) {
        return;
    }

    selectedFiles.forEach((file, index) => {
        const fileItem = document.createElement('div');
        fileItem.className = 'file-item';
        
        const fileName = document.createElement('span');
        fileName.className = 'file-name';
        fileName.textContent = file.name;
        
        const fileSize = document.createElement('span');
        fileSize.className = 'file-size';
        fileSize.textContent = formatFileSize(file.size);
        
        fileItem.appendChild(fileName);
        fileItem.appendChild(fileSize);
        fileListDiv.appendChild(fileItem);
    });
}

// 格式化文件大小
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 統一的論文生成函數
async function generatePaper(request, files = null) {
    console.log('開始生成論文...', { request, filesCount: files?.length || 0 });
    
    try {
        const generateBtn = document.getElementById('generateBtn');
        generateBtn.disabled = true;
        
        // 顯示結果區域
        document.getElementById('resultSection').style.display = 'block';
        hideMessages();
        document.getElementById('streamContent').style.display = 'block';
        document.getElementById('streamIndicator').style.display = 'block';
        
        const streamDiv = document.getElementById('streamContent');
        streamDiv.textContent = '';

        // 準備 FormData
        const generateFormData = new FormData();
        
        // 添加文件（如果有）
        if (files && files.length > 0) {
            generateBtn.innerHTML = '<span>📁</span><span>處理文件中...</span>';
            document.getElementById('status').textContent = '🚀 基於文件內容生成論文...';
            
            files.forEach(file => {
                generateFormData.append('files', file);
            });
        } else {
            generateBtn.innerHTML = '<span>⚡</span><span>流式生成中...</span>';
            document.getElementById('status').textContent = '🚀 開始流式生成...';
        }
        
        // 添加表單數據
        generateFormData.append('topic', request.topic);
        generateFormData.append('type', request.type);
        generateFormData.append('field', request.field);
        generateFormData.append('language', request.language);
        generateFormData.append('requirements', request.requirements || '');

        console.log('發送請求到 /api/papers/generate');
        await startStreamGeneration('/api/papers/generate', generateFormData, streamDiv);

    } catch (error) {
        console.error('generatePaper 錯誤:', error);
        showError('生成論文時發生錯誤: ' + error.message);
        resetForm();
    }
}

// 開始流式生成
async function startStreamGeneration(url, formData, streamDiv) {
    try {
        console.log('發送請求到:', url);
        
        const response = await fetch(url, {
            method: 'POST',
            body: formData
        });

        console.log('響應狀態:', response.status);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('請求失敗:', response.status, errorText);
            throw new Error(`請求失敗 (${response.status}): ${errorText}`);
        }

        await processStreamResponse(response, streamDiv);
        
    } catch (error) {
        console.error('startStreamGeneration 錯誤:', error);
        throw error;
    }
}

// 處理流式響應
async function processStreamResponse(response, streamDiv) {
    try {
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        console.log('開始處理流式響應...');

        while (true) {
            const { done, value } = await reader.read();
            
            if (done) {
                console.log('流式生成完成');
                document.getElementById('streamIndicator').style.display = 'none';
                document.getElementById('status').textContent = '✅ 流式生成完成！';
                resetForm();
                break;
            }

            const chunk = decoder.decode(value, { stream: true });
            buffer += chunk;
            
            // 顯示內容
            streamDiv.textContent = buffer;
            streamDiv.scrollTop = streamDiv.scrollHeight; // 自動滾動到底部
        }
        
    } catch (error) {
        console.error('processStreamResponse 錯誤:', error);
        throw error;
    }
}

// 顯示錯誤訊息
function showError(message) {
    console.error('顯示錯誤:', message);
    hideMessages();
    const errorDiv = document.getElementById('errorMessage');
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }
}

// 顯示成功訊息
function showSuccess(message) {
    console.log('顯示成功:', message);
    hideMessages();
    const successDiv = document.getElementById('successMessage');
    if (successDiv) {
        successDiv.textContent = message;
        successDiv.style.display = 'block';
    }
}

// 隱藏所有訊息
function hideMessages() {
    const errorDiv = document.getElementById('errorMessage');
    const successDiv = document.getElementById('successMessage');
    
    if (errorDiv) errorDiv.style.display = 'none';
    if (successDiv) successDiv.style.display = 'none';
}

// 重置表單狀態
function resetForm() {
    const generateBtn = document.getElementById('generateBtn');
    if (generateBtn) {
        generateBtn.disabled = false;
    }
    
    // 根據當前模式重置按鈕內容
    updateGenerationMode();
}
