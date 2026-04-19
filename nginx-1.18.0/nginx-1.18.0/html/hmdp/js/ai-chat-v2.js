/**
 * AI 智能客服聊天组件
 * 使用方法：在页面中引入此JS文件，并添加对应的HTML结构
 */
Vue.component('ai-chat', {
    template: `
        <div class="ai-chat-container">
            <!-- 聊天按钮 -->
            <div class="ai-chat-btn" @click="toggleChat" v-show="!isOpen">
                <i class="el-icon-chat-dot-round"></i>
                <span class="ai-chat-tip" v-if="!hasRead">AI客服</span>
            </div>
            
            <!-- 聊天窗口 -->
            <div class="ai-chat-window" v-show="isOpen">
                <div class="ai-chat-header">
                    <span>🤖 AI智能客服</span>
                    <i class="el-icon-close" @click="toggleChat"></i>
                </div>
                <div class="ai-chat-messages" ref="messagesContainer">
                    <div class="ai-message" :class="msg.role" v-for="(msg, index) in messages" :key="index">
                        <div class="ai-message-avatar">
                            <span v-if="msg.role === 'user'">我</span>
                            <span v-else>AI</span>
                        </div>
                        <div class="ai-message-content" v-html="formatMessage(msg.content)"></div>
                    </div>
                    <div class="ai-typing" v-if="isLoading">
                        <span class="ai-typing-dot"></span>
                        <span class="ai-typing-dot"></span>
                        <span class="ai-typing-dot"></span>
                    </div>
                </div>
                <div class="ai-chat-input">
                    <el-input 
                        v-model="inputMessage" 
                        placeholder="请输入您的问题..." 
                        @keyup.enter.native="sendMessage"
                        :disabled="isLoading">
                    </el-input>
                    <el-button type="primary" @click="sendMessage" :loading="isLoading" circle>
                        <i class="el-icon-s-promotion"></i>
                    </el-button>
                </div>
            </div>
        </div>
    `,
    data() {
        return {
            isOpen: false,
            hasRead: false,
            isLoading: false,
            inputMessage: '',
            messages: [
                {
                    role: 'assistant',
                    content: '您好！我是大众点评AI智能客服，有什么可以帮您的吗？您可以问我：\n• 推荐热门商铺\n• 查找某家店铺\n• 查询店铺优惠券'
                }
            ]
        }
    },
    methods: {
        toggleChat() {
            this.isOpen = !this.isOpen;
            if (this.isOpen) {
                this.hasRead = true;
                this.$nextTick(() => {
                    this.scrollToBottom();
                });
            }
        },
        async sendMessage() {
            if (!this.inputMessage.trim() || this.isLoading) return;
            
            const userMessage = this.inputMessage.trim();
            this.messages.push({ role: 'user', content: userMessage });
            this.inputMessage = '';
            this.isLoading = true;
            this.scrollToBottom();
            
            try {
                const response = await axios.post('/api/ai/chat', userMessage, {
                    headers: { 'Content-Type': 'text/plain' }
                });
                
                console.log('AI响应完整数据:', response);
                console.log('AI响应data:', response.data);
                console.log('AI响应data类型:', typeof response.data);
                console.log('AI响应data.success:', response.data ? response.data.success : 'undefined');
                
                // 检查响应数据
                if (!response.data) {
                    console.error('响应数据为空');
                    this.messages.push({ role: 'assistant', content: '抱歉，服务器返回数据为空。' });
                    return;
                }
                
                // 如果返回的是字符串（直接返回了AI回复）
                if (typeof response.data === 'string') {
                    console.log('返回的是字符串，直接显示');
                    this.messages.push({ role: 'assistant', content: response.data });
                    return;
                }
                
                // 正常返回 Result 对象
                if (response.data.success === true) {
                    console.log('成功，显示AI回复:', response.data.data);
                    this.messages.push({ role: 'assistant', content: response.data.data });
                } else {
                    console.log('失败，显示错误:', response.data.errorMsg);
                    const errorMsg = response.data.errorMsg || '抱歉，服务暂时不可用，请稍后再试。';
                    this.messages.push({ role: 'assistant', content: errorMsg });
                }
            } catch (error) {
                console.error('AI聊天出错:', error);
                console.error('错误详情:', error.response ? error.response.data : '无响应');
                this.messages.push({ role: 'assistant', content: '抱歉，网络出现异常，请稍后再试。' });
            } finally {
                this.isLoading = false;
                this.$nextTick(() => {
                    this.scrollToBottom();
                });
            }
        },
        scrollToBottom() {
            const container = this.$refs.messagesContainer;
            if (container) {
                container.scrollTop = container.scrollHeight;
            }
        },
        formatMessage(content) {
            // 简单的换行处理
            return content.replace(/\n/g, '<br>');
        }
    }
});

// 添加样式
const style = document.createElement('style');
style.textContent = `
    .ai-chat-container {
        position: fixed;
        bottom: 80px;
        right: 20px;
        z-index: 9999;
    }
    
    .ai-chat-btn {
        width: 56px;
        height: 56px;
        border-radius: 50%;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
        transition: transform 0.3s, box-shadow 0.3s;
        font-size: 24px;
        position: relative;
    }
    
    .ai-chat-btn:hover {
        transform: scale(1.1);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
    }
    
    .ai-chat-tip {
        position: absolute;
        top: -8px;
        right: -8px;
        background: #ff4757;
        color: white;
        font-size: 10px;
        padding: 2px 6px;
        border-radius: 10px;
        white-space: nowrap;
    }
    
    .ai-chat-window {
        width: 360px;
        height: 500px;
        background: white;
        border-radius: 16px;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
        display: flex;
        flex-direction: column;
        overflow: hidden;
        margin-bottom: 10px;
    }
    
    .ai-chat-header {
        padding: 16px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 16px;
        font-weight: 600;
    }
    
    .ai-chat-header i {
        cursor: pointer;
        font-size: 18px;
    }
    
    .ai-chat-messages {
        flex: 1;
        padding: 16px;
        overflow-y: auto;
        background: #f8f9fa;
    }
    
    .ai-message {
        display: flex;
        margin-bottom: 12px;
        align-items: flex-start;
    }
    
    .ai-message.user {
        flex-direction: row-reverse;
    }
    
    .ai-message-avatar {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        font-weight: bold;
        flex-shrink: 0;
    }
    
    .ai-message.user .ai-message-avatar {
        background: #667eea;
        color: white;
        margin-left: 8px;
    }
    
    .ai-message.assistant .ai-message-avatar {
        background: #e9ecef;
        color: #495057;
        margin-right: 8px;
    }
    
    .ai-message-content {
        max-width: 75%;
        padding: 10px 14px;
        border-radius: 16px;
        font-size: 14px;
        line-height: 1.5;
        word-wrap: break-word;
    }
    
    .ai-message.user .ai-message-content {
        background: #667eea;
        color: white;
        border-bottom-right-radius: 4px;
    }
    
    .ai-message.assistant .ai-message-content {
        background: white;
        color: #333;
        border-bottom-left-radius: 4px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }
    
    .ai-typing {
        display: flex;
        justify-content: center;
        padding: 10px;
    }
    
    .ai-typing-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #667eea;
        margin: 0 3px;
        animation: typing 1.4s infinite ease-in-out;
    }
    
    .ai-typing-dot:nth-child(1) { animation-delay: 0s; }
    .ai-typing-dot:nth-child(2) { animation-delay: 0.2s; }
    .ai-typing-dot:nth-child(3) { animation-delay: 0.4s; }
    
    @keyframes typing {
        0%, 80%, 100% { transform: scale(0.8); opacity: 0.5; }
        40% { transform: scale(1.2); opacity: 1; }
    }
    
    .ai-chat-input {
        padding: 12px;
        display: flex;
        gap: 8px;
        border-top: 1px solid #eee;
        background: white;
    }
    
    .ai-chat-input .el-input {
        flex: 1;
    }
    
    @media (max-width: 400px) {
        .ai-chat-window {
            width: calc(100vw - 40px);
            height: 60vh;
        }
    }
`;
document.head.appendChild(style);
