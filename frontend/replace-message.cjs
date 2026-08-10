const fs = require('fs');
const path = require('path');

function walkDir(dir, callback) {
  fs.readdirSync(dir).forEach(f => {
    let dirPath = path.join(dir, f);
    let isDirectory = fs.statSync(dirPath).isDirectory();
    isDirectory ? walkDir(dirPath, callback) : callback(path.join(dir, f));
  });
}

walkDir('./src', (filePath) => {
  if (!filePath.endsWith('.tsx') && !filePath.endsWith('.ts')) return;
  let content = fs.readFileSync(filePath, 'utf8');
  let changed = false;

  if (content.includes('message.success') || content.includes('message.error') || content.includes('message.info') || content.includes('message.warning')) {
    content = content.replace(/message\.(success|error|info|warning)/g, 'toast.$1');
    changed = true;
  }

  if (changed) {
    if (content.includes('from \'sonner\'') || content.includes('from "sonner"')) {
      // already imported
    } else {
      content = 'import { toast } from "sonner";\n' + content;
    }
    
    // Remove message from antd import
    content = content.replace(/import\s+\{\s*([^}]*)\s*\}\s+from\s+['"]antd['"]/g, (match, p1) => {
      const parts = p1.split(',').map(p => p.trim()).filter(p => p && p !== 'message');
      if (parts.length === 0) return '';
      return 'import { ' + parts.join(', ') + ' } from \'antd\'';
    });

    fs.writeFileSync(filePath, content, 'utf8');
    console.log('Updated ' + filePath);
  }
});
