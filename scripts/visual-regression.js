#!/usr/bin/env node

/**
 * Visual Regression Testing Script
 * Compares screenshots against baseline images using pixel-by-pixel comparison
 * Usage: node scripts/visual-regression.js --baselines <dir> --screenshots <dir> [--threshold <num>] [--output <dir>]
 */

const fs = require('fs');
const path = require('path');
const { PNG } = require('pngjs');
const pixelmatch = require('pixelmatch');

// Parse command line arguments
function parseArgs() {
  const args = process.argv.slice(2);
  const options = {
    baselines: null,
    screenshots: null,
    threshold: 0.1,
    output: null,
  };

  for (let i = 0; i < args.length; i += 2) {
    const key = args[i].replace(/^--/, '');
    const value = args[i + 1];
    
    if (key === 'threshold') {
      options[key] = parseFloat(value);
    } else {
      options[key] = value;
    }
  }

  if (!options.baselines || !options.screenshots) {
    console.error('Usage: node visual-regression.js --baselines <dir> --screenshots <dir> [--threshold <num>] [--output <dir>]');
    process.exit(1);
  }

  return options;
}

// Read PNG image from file
function readImage(filePath) {
  return new Promise((resolve, reject) => {
    const stream = fs.createReadStream(filePath).pipe(new PNG());
    stream.on('parsed', function() {
      resolve(this);
    });
    stream.on('error', reject);
  });
}

// Write PNG image to file
function writeImage(filePath, png) {
  return new Promise((resolve, reject) => {
    const stream = png.pack();
    const output = fs.createWriteStream(filePath);
    stream.pipe(output);
    output.on('finish', resolve);
    output.on('error', reject);
  });
}

// Compare two images and return diff percentage
async function compareImages(baselinePath, screenshotPath, outputPath) {
  try {
    const baseline = await readImage(baselinePath);
    const screenshot = await readImage(screenshotPath);

    const { width, height } = baseline;
    
    // Check dimensions match
    if (width !== screenshot.width || height !== screenshot.height) {
      console.warn(`  ⚠️  Dimension mismatch: ${path.basename(baselinePath)} (${width}x${height} vs ${screenshot.width}x${screenshot.height})`);
      return null;
    }

    // Create diff image
    const diff = new PNG({ width, height });
    
    // Compare pixels
    const numDiffPixels = pixelmatch(
      baseline.data,
      screenshot.data,
      diff.data,
      width,
      height,
      { threshold: 0.1 }
    );

    // Calculate difference percentage
    const totalPixels = width * height;
    const diffPercentage = (numDiffPixels / totalPixels) * 100;

    // Save diff image if output directory specified
    if (outputPath) {
      const diffFileName = path.basename(baselinePath, path.extname(baselinePath)) + '-diff.png';
      const diffPath = path.join(outputPath, diffFileName);
      await writeImage(diffPath, diff);
    }

    return {
      diffPixels: numDiffPixels,
      totalPixels,
      diffPercentage,
      matchPercentage: 100 - diffPercentage,
    };
  } catch (error) {
    console.error(`  ❌ Error comparing ${path.basename(baselinePath)}: ${error.message}`);
    return null;
  }
}

// Main function
async function main() {
  const options = parseArgs();

  console.log('🔍 Visual Regression Testing');
  console.log(`   Baselines:    ${options.baselines}`);
  console.log(`   Screenshots:  ${options.screenshots}`);
  console.log(`   Threshold:    ${(options.threshold * 100).toFixed(1)}%`);
  if (options.output) {
    console.log(`   Diff output:  ${options.output}`);
    
    // Ensure output directory exists
    if (!fs.existsSync(options.output)) {
      fs.mkdirSync(options.output, { recursive: true });
    }
  }
  console.log('');

  // Get baseline images
  if (!fs.existsSync(options.baselines)) {
    console.error(`❌ Baselines directory not found: ${options.baselines}`);
    process.exit(1);
  }

  if (!fs.existsSync(options.screenshots)) {
    console.error(`❌ Screenshots directory not found: ${options.screenshots}`);
    process.exit(1);
  }

  const baselineFiles = fs.readdirSync(options.baselines)
    .filter(f => f.endsWith('.png'));

  if (baselineFiles.length === 0) {
    console.log('⚠️  No baseline images found. Run Maestro flows and use npm run update-baselines to create baselines.');
    process.exit(0);
  }

  console.log(`Found ${baselineFiles.length} baseline images\n`);
  console.log('Results:');
  console.log('─'.repeat(80));

  const results = [];
  let hasFailures = false;

  for (const baselineFile of baselineFiles) {
    const baselinePath = path.join(options.baselines, baselineFile);
    const screenshotPath = path.join(options.screenshots, baselineFile);

    // Check if matching screenshot exists
    if (!fs.existsSync(screenshotPath)) {
      console.log(`⚠️  ${baselineFile.padEnd(50)} | MISSING`);
      results.push({ file: baselineFile, status: 'MISSING', match: null });
      continue;
    }

    const result = await compareImages(
      baselinePath,
      screenshotPath,
      options.output
    );

    if (result === null) {
      results.push({ file: baselineFile, status: 'ERROR', match: null });
      hasFailures = true;
      continue;
    }

    const matchPercent = result.matchPercentage.toFixed(2);
    const diffPercent = result.diffPercentage.toFixed(2);
    const thresholdPercent = (options.threshold * 100).toFixed(1);
    
    const passed = result.diffPercentage <= (options.threshold * 100);
    const status = passed ? '✅ PASS' : '❌ FAIL';
    
    if (!passed) {
      hasFailures = true;
    }

    console.log(`${status} ${baselineFile.padEnd(45)} | ${matchPercent}% match | ${diffPercent}% diff`);
    results.push({ 
      file: baselineFile, 
      status: passed ? 'PASS' : 'FAIL',
      match: matchPercent,
      diff: diffPercent,
    });
  }

  console.log('─'.repeat(80));
  console.log('');

  // Summary
  const passed = results.filter(r => r.status === 'PASS').length;
  const failed = results.filter(r => r.status === 'FAIL').length;
  const missing = results.filter(r => r.status === 'MISSING').length;
  const errors = results.filter(r => r.status === 'ERROR').length;

  console.log('Summary:');
  console.log(`  ✅ Passed:  ${passed}`);
  if (failed > 0) console.log(`  ❌ Failed:  ${failed}`);
  if (missing > 0) console.log(`  ⚠️  Missing: ${missing}`);
  if (errors > 0) console.log(`  ❌ Errors:  ${errors}`);
  console.log('');

  if (hasFailures) {
    console.log('❌ Visual regression test failed - differences exceed threshold');
    if (options.output) {
      console.log(`   Check diff images in: ${options.output}`);
    }
    process.exit(1);
  } else if (missing > 0 || errors > 0) {
    console.log('⚠️  Completed with warnings');
    process.exit(0);
  } else {
    console.log('✅ All visual regression tests passed!');
    process.exit(0);
  }
}

main().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
