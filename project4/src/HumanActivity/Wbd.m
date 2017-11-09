function Y = Wbd(X)
% Calculates the binned distribution
    Y = zeros(length(X), 10);
    for i = 1:length(X)
        Y(i,:) = histcounts(X(i,:), 10, 'Normalization', 'probability');
    end
end