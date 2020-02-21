'use strict';
// The module 'vscode' contains the VS Code extensibility API
// Import the module and reference it with the alias vscode in your code below
import * as vscode from 'vscode';
import { readdirSync } from 'fs';

let jvmcode

// this method is called when your extension is activated
// your extension is activated the very first time the command is executed
export function activate(context: vscode.ExtensionContext) {

    jvmcode = vscode.extensions.getExtension('contrapt.jvmcode').exports

    installVerticle()

    function installVerticle() {
        let jarFiles = findJars().map((j) => { return context.asAbsolutePath('out/' + j) })
        jvmcode.install(jarFiles, 'net.contrapt.kotlin.KotlinVerticle').then((result) => {
            registerProviders()
        }).catch((error) => {
            vscode.window.showErrorMessage('Error starting maven service: ' + error.message)
        })
    }

    function findJars() {
        let files = []
        let dir = context.asAbsolutePath('out')
        readdirSync(dir).forEach((entry) => {
            if (entry.endsWith('.jar')) {
                files.push(entry)
            }
        })
        return files
    }

    function registerProviders() {
        // Do we have a task provider??
    }
}

// this method is called when your extension is deactivated
export function deactivate() {
    console.log('Closing all the things')
}
