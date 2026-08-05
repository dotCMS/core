<script setup lang="ts">
import { reactive, ref } from 'vue';

defineProps<{ description?: string }>();

const initialFormData = () => ({ firstName: '', lastName: '', email: '', acceptTerms: false });

const formData = reactive(initialFormData());
const isSubmitting = ref(false);
const isSuccess = ref(false);

const resetForm = () => Object.assign(formData, initialFormData());

// Emulate a form submission.
const handleSubmit = () => {
    isSubmitting.value = true;
    setTimeout(() => {
        isSubmitting.value = false;
        isSuccess.value = true;
        resetForm();
    }, 3000);
};
</script>

<template>
    <div class="max-w-lg mx-auto my-10 p-6 bg-white rounded-lg shadow-md">
        <h2 class="text-2xl font-bold text-gray-800 mb-6">Contact Us</h2>

        <div v-if="description" class="text-gray-700 mb-6">{{ description }}</div>

        <div
            v-if="isSuccess"
            class="mb-6 p-4 bg-green-100 border border-green-400 text-green-700 rounded">
            Thank you for contacting us! We'll get back to you soon.
        </div>

        <form class="space-y-4" @submit.prevent="handleSubmit">
            <div>
                <label for="firstName" class="block text-sm font-medium text-gray-700 mb-1">
                    First Name
                </label>
                <input
                    id="firstName"
                    v-model="formData.firstName"
                    type="text"
                    required
                    class="w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 border-gray-300" />
            </div>

            <div>
                <label for="lastName" class="block text-sm font-medium text-gray-700 mb-1">
                    Last Name
                </label>
                <input
                    id="lastName"
                    v-model="formData.lastName"
                    type="text"
                    required
                    class="w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 border-gray-300" />
            </div>

            <div>
                <label for="email" class="block text-sm font-medium text-gray-700 mb-1">
                    Email
                </label>
                <input
                    id="email"
                    v-model="formData.email"
                    type="email"
                    required
                    class="w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 border-gray-300" />
            </div>

            <div class="flex items-start">
                <div class="flex items-center h-5">
                    <input
                        id="acceptTerms"
                        v-model="formData.acceptTerms"
                        type="checkbox"
                        required
                        class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
                </div>
                <div class="ml-3">
                    <label for="acceptTerms" class="text-sm text-gray-700">
                        I accept the terms and conditions
                    </label>
                </div>
            </div>

            <div class="flex justify-between pt-4">
                <button
                    type="button"
                    class="px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-500"
                    @click="resetForm">
                    Reset
                </button>
                <button
                    type="submit"
                    :disabled="isSubmitting"
                    class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-75">
                    {{ isSubmitting ? 'Submitting...' : 'Submit' }}
                </button>
            </div>
        </form>
    </div>
</template>
