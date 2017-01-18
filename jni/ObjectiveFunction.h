//
// Created by admin on 07/01/2017.
//

#include "ObjectiveFunctionHelper.h"
#include "OrthogonalProjectionModel.h"

#ifndef SHAPE3D_OBEJCTIVEFUNCTION_H
#define SHAPE3D_OBEJCTIVEFUNCTION_H

class ObjectiveFunction
{
public:
    ObjectiveFunction(ObjectiveFunctionHelper &,
                      OrthogonalProjectionModel &projection_model);
    double operator()(const dlib::matrix<double> &arg) const;



private:
    ObjectiveFunctionHelper helper;

    dlib::matrix<double> shape2d;
    std::unordered_map<int,dlib::matrix<double> > blendshapes;
public:
    void extract2d_from_image(dlib::matrix<double> &image);
    void set(std::unordered_map<int,dlib::matrix<double> > cur_blendshapes);

    OrthogonalProjectionModel model;
};

#endif //SHAPE3D_OBEJCTIVEFUNCTION_H
