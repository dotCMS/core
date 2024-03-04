import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { AIImageSize } from './dot-ai.service';

import { PromptType } from '../../../extensions/ai-image-prompt/ai-image-prompt.models';

export interface AiPluginResponse {
    id: string;
    object: string;
    created: number;
    model: string;
    choices: Choice[];
    usage: Usage;
    system_fingerprint: null;
    totalTime: string;
    error?: DotAiError;
}

interface Choice {
    index: number;
    message: Message;
    finish_reason: string;
}
interface Message {
    role: string;
    content: string;
}
interface Usage {
    prompt_tokens: number;
    completion_tokens: number;
    total_tokens: number;
}

export interface DotAIImageResponse {
    originalPrompt: string;
    response: string;
    revised_prompt: string;
    tempFileName: string;
    url: string;
}

export interface AIImagePrompt {
    text: string;
    type: PromptType;
    size: AIImageSize;
}

export interface DotAIImageContent extends  DotAIImageResponse {
    contentlet: DotCMSContentlet;
}

export interface DotGeneratedAIImage {
    request:AIImagePrompt
    response:DotAIImageContent
}

export interface DotAICompletionsConfig {
    apiImageUrl: string;
    apiKey: string;
    apiUrl: string;
    availableModels: string[];
    configHost: string;
    imageModel: string;
    imagePrompt: string;
    imageSize: string;
    model: string;
    rolePrompt: string;
    textPrompt: string;
}

export interface DotAiError {
    code: string;
    message: string;
    param: string;
    type: string;
}
