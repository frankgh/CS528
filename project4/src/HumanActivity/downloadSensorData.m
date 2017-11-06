function downloadSensorData
% Download and extract data if data folder does not exist
% Copyright (c) 2015, MathWorks, Inc.

if ~(exist('UCI HAR Dataset','file') == 7)
    downloadlink = 'https://archive.ics.uci.edu/ml/machine-learning-databases/00240/UCI%20HAR%20Dataset.zip';
    tic
    disp('UCI HAR Dataset does not exist, downloading dataset...')
    fname = websave('UCI_HAR_Dataset.zip',downloadlink);
    toc
    disp('Done downloading, file downloaded to:')
    disp(fname)
    disp(' ')
    disp('It may be faster to extract the zip file manually using another software,')
    yn = input('Do you want MATLAB to extract the file for you (y/n)? ','s');
    if strcmp(yn,'y')
        tic
        disp('Extracting file, this may take a while...')
        foldername = unzip(fname);
        disp('Done extracting')
        toc
    else 
        disp(' ')
        disp('OK, You must manually extract the file contents to the current folder before proceeding.')
        disp('Please make sure that you don''t change the default folder name: ''UCI HAR Dataset''')
    end
end