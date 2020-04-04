'use strict';
// The module 'vscode' contains the VS Code extensibility API
// Import the module and reference it with the alias vscode in your code below
import * as vscode from 'vscode';
import { readdirSync } from 'fs';
import { KotlinProvider } from './kotlin_provider';

let jvmcode

// this method is called when your extension is activated
// your extension is activated the very first time the command is executed
export function activate(context: vscode.ExtensionContext) {

    jvmcode = vscode.extensions.getExtension('contrapt.jvmcode').exports
    let request = new KotlinProvider()
    jvmcode.requestLanguage(request)
}

// this method is called when your extension is deactivated
export function deactivate() {
    console.log('Closing all the things')
}
