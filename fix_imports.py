import os

kt_files = []
for root_dir, dirs, files in os.walk('app/src/main/java/com/deviceinfo/gad'):
    for file in files:
        if file.endswith('.kt'):
            kt_files.append(os.path.join(root_dir, file))

for kt_file in kt_files:
    with open(kt_file, 'r') as f:
        content = f.read()
    
    needs_string_res = 'stringResource' in content
    needs_r = 'R.string' in content or 'R.drawable' in content or 'R.mipmap' in content
    
    imports = []
    if needs_string_res and 'import androidx.compose.ui.res.stringResource' not in content:
        imports.append('import androidx.compose.ui.res.stringResource')
    if needs_r and 'import com.deviceinfo.gad.R' not in content:
        imports.append('import com.deviceinfo.gad.R')
    
    if imports:
        # Find the package line
        lines = content.split('\n')
        pkg_idx = 0
        for i, line in enumerate(lines):
            if line.startswith('package '):
                pkg_idx = i
                break
        
        for imp in imports:
            lines.insert(pkg_idx + 1, imp)
            
        with open(kt_file, 'w') as f:
            f.write('\n'.join(lines))
