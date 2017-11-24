function R = Wavgresacc(X,Y,Z)
% calculates the average_resultant_acceleration
    R = mean(sqrt(X.^2 + Y.^2 + Z.^2), 2);
end