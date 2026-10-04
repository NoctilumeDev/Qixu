import ts from 'typescript';

// Execute real source with injected dependencies. Imports/exports are removed
// through TypeScript's syntax tree, so formatting never controls test behavior.
export function compileInjected(source) {
  const tree = ts.createSourceFile('fixture.ts', source, ts.ScriptTarget.ES2022, true);
  const edits = [];
  for (const statement of tree.statements) {
    if (ts.isImportDeclaration(statement) || ts.isExportDeclaration(statement)) {
      edits.push([statement.getStart(tree), statement.end]);
    } else if (ts.isExportAssignment(statement)) {
      throw new Error('Injected harness does not support export assignments');
    } else {
      for (const modifier of statement.modifiers ?? []) {
        if ([ts.SyntaxKind.ExportKeyword, ts.SyntaxKind.DefaultKeyword].includes(modifier.kind)) {
          edits.push([modifier.getStart(tree), modifier.end]);
        }
      }
    }
  }
  for (const [start, end] of edits.sort((a, b) => b[0] - a[0])) {
    source = source.slice(0, start) + source.slice(end);
  }
  const result = ts.transpileModule(source, {
    reportDiagnostics: true,
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext },
  });
  if (result.diagnostics?.some((d) => d.category === ts.DiagnosticCategory.Error)) {
    throw new Error(
      ts.formatDiagnosticsWithColorAndContext(result.diagnostics, {
        getCurrentDirectory: () => '',
        getCanonicalFileName: (x) => x,
        getNewLine: () => '\n',
      }),
    );
  }
  return result.outputText;
}

export function setupScript(sfc) {
  const match = sfc.match(/<script\s+setup\s+lang="ts">([\s\S]*?)<\/script>/);
  if (!match) throw new Error('Actual script setup not found');
  return compileInjected(match[1]);
}

export function findElement(tree, predicate) {
  for (const child of tree.children ?? []) {
    if (child.type === 1 && predicate(child)) return child;
    const found = findElement(child, predicate);
    if (found) return found;
  }
  return null;
}
