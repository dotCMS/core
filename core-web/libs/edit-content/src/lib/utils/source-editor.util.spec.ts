import { describe, expect, it } from 'vitest';

import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotContentletCanLock
} from '@dotcms/dotcms-models';
import { DOT_VELOCITY_LANGUAGE_ID } from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { toSourceEditorRequest } from './source-editor.util';

const fileRow = (overrides: Partial<DotCMSContentlet> = {}) =>
    createFakeContentlet({
        baseType: DotCMSBaseTypesContentTypes.FILEASSET,
        extension: 'vtl',
        fileName: 'header.vtl',
        ...overrides
    });

const lock = (canLock: boolean, locked = false): DotContentletCanLock => ({
    canLock,
    locked,
    id: 'identifier',
    inode: 'inode',
    lockedBy: locked ? 'someone@dotcms.com' : ''
});

const CAN_LOCK = lock(true);

describe('toSourceEditorRequest', () => {
    it('opens a Velocity file the user can lock, named by its file name', () => {
        const file = fileRow({ title: 'Site header' });

        expect(toSourceEditorRequest(file, CAN_LOCK)).toEqual({
            inode: file.inode,
            identifier: file.identifier,
            languageId: file.languageId,
            title: 'header.vtl',
            language: DOT_VELOCITY_LANGUAGE_ID
        });
    });

    it('opens a file the user already holds the lock on', () => {
        expect(toSourceEditorRequest(fileRow(), lock(true, true))).not.toBeNull();
    });

    it('does not open a file the user cannot lock: no edit permission, or locked by someone else', () => {
        expect(toSourceEditorRequest(fileRow(), lock(false))).toBeNull();
        expect(toSourceEditorRequest(fileRow(), lock(false, true))).toBeNull();
    });

    it('reads the extension case-insensitively', () => {
        expect(toSourceEditorRequest(fileRow({ extension: 'VTL' }), CAN_LOCK)?.language).toBe(
            DOT_VELOCITY_LANGUAGE_ID
        );
    });

    it('falls back to the file name when the row carries no extension', () => {
        expect(toSourceEditorRequest(fileRow({ extension: undefined }), CAN_LOCK)?.language).toBe(
            DOT_VELOCITY_LANGUAGE_ID
        );
    });

    it('falls back to the title when the row carries no file name', () => {
        const file = fileRow({ fileName: undefined, title: 'header.vtl' });

        expect(toSourceEditorRequest(file, CAN_LOCK)?.title).toBe('header.vtl');
    });

    it('does not open a file named after the extension but without one', () => {
        expect(
            toSourceEditorRequest(fileRow({ extension: undefined, fileName: 'vtl' }), CAN_LOCK)
        ).toBeNull();
    });

    it('does not open file types it does not handle yet', () => {
        expect(toSourceEditorRequest(fileRow({ extension: 'png' }), CAN_LOCK)).toBeNull();
    });

    it('does not resolve an extension that names an object member', () => {
        expect(toSourceEditorRequest(fileRow({ extension: 'constructor' }), CAN_LOCK)).toBeNull();
    });

    it('does not open content that is not a File Asset, even with a .vtl name', () => {
        expect(
            toSourceEditorRequest(
                fileRow({ baseType: DotCMSBaseTypesContentTypes.DOTASSET }),
                CAN_LOCK
            )
        ).toBeNull();
    });
});
